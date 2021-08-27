package fi.ylihallila.server.controllers;

import com.google.gson.Gson;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.models.Project;
import fi.ylihallila.server.models.Subject;
import fi.ylihallila.server.models.User;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.*;
import io.javalin.plugin.openapi.annotations.*;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class ProjectController extends Controller implements CrudHandler {

	private Logger logger = LoggerFactory.getLogger(ProjectController.class);

	@OpenApi(
		tags = { "projects" },
		summary = "Create a new (personal) project",
		queryParams = {
			@OpenApiParam(name = "personal", description = "If this parameter is set, the created project will be a personal project."),
		},
		formParams = {
			@OpenApiFormParam(name = "project-name", required = true),
			@OpenApiFormParam(name = "subject-id"),
			@OpenApiFormParam(name = "description"),
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404"),
		}
	)
	@Override public void create(@NotNull Context ctx) {
		Allow(ctx, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS);

		if (ctx.queryParamMap().containsKey("personal")) {
			createPersonalProject(ctx);
		} else {
			createProject(ctx);
		}
	}

	@OpenApi(
		tags = { "projects" },
		summary = "Update given project",
		pathParams = {
			@OpenApiParam(name = "id", required = true),
		},
		formParams = {
			@OpenApiFormParam(name = "name"),
			@OpenApiFormParam(name = "description"),
			@OpenApiFormParam(name = "hidden", type = boolean.class),
		},
		responses = {
			@OpenApiResponse(status = "200", content = @OpenApiContent(from = Project.class)),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404")
		}
	)
	@Override public void update(@NotNull Context ctx, @NotNull String id) {
		Allow(ctx, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS);

		Session session = ctx.use(Session.class);
		User user       = Authenticator.getUser(ctx);

		Project project = session.find(Project.class, id);

		if (project == null) {
			throw new NotFoundResponse();
		}

		if (!(project.hasPermission(user))) {
			throw new ForbiddenResponse();
		}

		project.setName(ctx.formParam("name", project.getName()));
		project.setDescription(ctx.formParam("description", project.getDescription()));
		project.setHidden(validate(ctx, "hidden", Boolean.class, project.isHidden()));
		project.setModifiedAt(System.currentTimeMillis());

		session.update(project);

		ctx.status(200).json(project);

		logger.info("Project {} ({}) edited by {} ({})", project.getName(), project.getId(), user.getName(), user.getId());
	}

	@OpenApi(
		tags = { "projects" },
		summary = "Delete given project.",
		pathParams = {
			@OpenApiParam(name = "id", required = true),
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404"),
		}
	)
	@Override public void delete(@NotNull Context ctx, @NotNull String id) {
		Allow(ctx, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS);

		Session session = ctx.use(Session.class);
		User user       = Authenticator.getUser(ctx);

		Project project = session.find(Project.class, id);

		if (project == null) {
			throw new NotFoundResponse();
		}

		if (!(project.hasPermission(user))) {
			throw new ForbiddenResponse();
		}

		// Hibernate requires removing the association prior to deleting the project from the database.
		project.getSubject().removeProject(project);

		session.delete(project);
		backupAndDelete(getProjectFile(id));

		ctx.status(200);

		logger.info("Project {} deleted by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));

	}

	@OpenApi(
		tags = { "projects" },
		summary = "Fetch given project data file. This returns the QuPath project file, not the database representation of this project.",
		pathParams = {
			@OpenApiParam(name = "id", required = true),
			@OpenApiParam(name = "timestamp", description = "Timestamp of backup; optional."),
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "404")
		}
	)
	@Override public void getOne(@NotNull Context ctx, @NotNull String id) {
		File file;

		if (ctx.queryParamMap().containsKey("timestamp")) {
			file = new File(getBackupFile(id + ".json", ctx.queryParam("timestamp")));
		} else {
			file = new File(getProjectFile(id));
		}

		if (!file.exists()) {
			throw new NotFoundResponse();
		}

		try {
			// File is already JSON encoded so we cannot use ctx.json();
			ctx.status(200).contentType("application/json").result(Files.readString(file.toPath()));
		} catch (IOException e) {
			logger.error("Error while reading project file", e);
			throw new InternalServerErrorResponse(e.getMessage());
		}
	}

	@OpenApi(
		tags = { "projects" },
		summary = "Fetch all projects.",
		responses = {
			@OpenApiResponse(status = "200", content = @OpenApiContent(from = Project.class, isArray = true))
		}
	)
	@Override public void getAll(@NotNull Context ctx) {
		Allow(ctx, Roles.ADMIN);

		Session session = ctx.use(Session.class);

		List<Project> projects = session.createQuery("from Project", Project.class).list();

		ctx.status(200).json(projects);
	}

	@OpenApi(
		tags = { "projects" },
		summary = "Upload a new version of a project.",
		pathParams = {
			@OpenApiParam(name = "id", required = true),
		},
		formParams = {
			@OpenApiFormParam(name = "project-data", required = true)
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "403")
		},
		method = HttpMethod.POST,
		path = "/api/v0/projects/:id"
	)
	public void uploadProject(Context ctx) throws IOException {
		Allow(ctx, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS);

		String projectData = ctx.formParam("project-data", String.class).get();
		String id          = ctx.pathParam("id", String.class).get();
		Session session    = ctx.use(Session.class);
		User user          = Authenticator.getUser(ctx);

		Project project = session.find(Project.class, id);

		if (project == null) {
			throw new NotFoundResponse();
		}

		if (!(project.hasPermission(user))) {
			throw new UnauthorizedResponse();
		}

		project.setModifiedAt(System.currentTimeMillis());
		session.update(project);

		try (InputStream is = new ByteArrayInputStream(projectData.getBytes(StandardCharsets.UTF_8))) {
			copyInputStreamToFile(is, new File(getProjectFile(id)));
			backup(getProjectFile(id));
		}

		logger.info("Project {} ({}) updated by {} ({})", project.getName(), project.getId(), user.getName(), user.getId());
	}

	/* Private API */

	private void createProject(Context ctx) {
		String projectName = ctx.formParam("project-name", String.class).get();
		String subjectId   = ctx.formParam("subject-id",  String.class).get();
		String projectId   = UUID.randomUUID().toString();
		User user = Authenticator.getUser(ctx);
		Session session = ctx.use(Session.class);

		Subject subject = session.find(Subject.class, subjectId);

		if (subject == null) {
			throw new NotFoundResponse();
		}

		if (!subject.getWorkspace().hasPermission(user)) {
			throw new UnauthorizedResponse();
		}

		createProjectJsonFile(projectId);

		Project project = new Project();
		project.setId(projectId);
		project.setName(projectName);
		project.setDescription(ctx.formParam("description", ""));

		subject.addProject(project);

		logger.info("Project {} created by {}", projectId, user.getName());
	}

	public void createPersonalProject(Context ctx) {
		String projectName = ctx.formParam("project-name", String.class).get();
		String projectId = UUID.randomUUID().toString();
		User user = Authenticator.getUser(ctx);
		Session session = ctx.use(Session.class);

		createProjectJsonFile(projectId);

		Project project = new Project();
		project.setName("Copy of " + projectName);
		project.setId(projectId);
		project.setSubject(user.getCopiedProjectsSubject());
		session.save(project);

		ctx.status(200).html(projectId);

		logger.info("Personal project {} created by {}", projectId, user.getName());
	}

	private void createProjectJsonFile(String projectId) {
		try {
			Path projectFile = Path.of(getProjectFile(projectId));
			String JSON = new Gson().toJson(new EmptyProject(projectId));

			Files.writeString(projectFile, JSON);
		} catch (IOException e) {
			logger.error("Error while creating project file");
			throw new InternalServerErrorResponse(e.getMessage());
		}
	}

	/**
	 * Represents the very bare-bones of a QuPath Project file.
	 */
	static class EmptyProject {

		private String version = "1";
		private String id;
		private Long createTimestamp = System.currentTimeMillis();
		private Long modifyTimestamp = System.currentTimeMillis();
		private String metadata = "";
		private String[] images = {};

		public EmptyProject(String id) {
			this.id = id;
		}

		public String getVersion() {
			return version;
		}

		public String getId() {
			return id;
		}

		public Long getCreateTimestamp() {
			return createTimestamp;
		}

		public Long getModifyTimestamp() {
			return modifyTimestamp;
		}

		public String getMetadata() {
			return metadata;
		}

		public String[] getImages() {
			return images;
		}
	}
}
