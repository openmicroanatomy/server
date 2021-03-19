package fi.ylihallila.server.controllers;

import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.models.Project;
import fi.ylihallila.server.models.Subject;
import fi.ylihallila.server.models.User;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.hibernate.Session;
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

public class ProjectController extends Controller {

	private Logger logger = LoggerFactory.getLogger(ProjectController.class);

	public void getAllProjects(Context ctx) {
		Session session = ctx.use(Session.class);

		List<Project> projects = session.createQuery("from Project", Project.class).list();

		ctx.status(200).json(projects);
	}

	public void createPersonalProject(Context ctx) throws IOException {
//		TODO: Rework. Personal projects now belong to "[Name]'s workspace" with default subject "Personal projects"

		String projectName = ctx.formParam("project-name", String.class).get();
		String projectId = UUID.randomUUID().toString();
		User user = Authenticator.getUser(ctx);
		Session session = ctx.use(Session.class);

		Project project = new Project();
		project.setName("Copy of " + projectName);
		project.setId(projectId);
		project.getSubject().getWorkspace().setOwner(user);
		session.save(project);

		createProjectJsonFile(projectId);

		ctx.status(200).html(projectId);

		logger.info("Personal project {} created by {}", projectId, user.getName());
	}

	public void createProject(Context ctx) throws IOException {
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

		Project project = new Project();
		project.setId(projectId);
		project.setName(projectName);
		project.setDescription(ctx.formParam("description", ""));

		subject.addProject(project);

		createProjectJsonFile(projectId);

		logger.info("Project {} created by {}", projectId, user.getName());
	}

	public void downloadProject(Context ctx) throws IOException {
		String id = ctx.pathParam("project-id", String.class).get();

		File file;

		if (ctx.queryParamMap().containsKey("timestamp")) { // TODO: Backup JSON files
			file = new File(getBackupFile(id + ".json", ctx.queryParam("timestamp")));
		} else {
			file = new File(getProjectFile(id));
		}

		if (!file.exists()) {
			throw new NotFoundResponse();
		}

		ctx.status(200).contentType("application/json").result(Files.readString(file.toPath()));
	}

	// TODO: Doesn't check if project exist?
	public void updateProject(Context ctx) {
		String id = ctx.pathParam("project-id", String.class).get();

		if (!hasPermission(ctx, id)) {
			throw new ForbiddenResponse();
		}

		Session session = ctx.use(Session.class);

		Project project = session.find(Project.class, id);
		project.setName(ctx.formParam("name", project.getName()));
		project.setDescription(ctx.formParam("description", project.getDescription()));
		project.setHidden(validate(ctx, "hidden", Boolean.class, project.isHidden()));
		project.setModifiedAt(System.currentTimeMillis());
		session.update(project);

		ctx.status(200);

		logger.info("Project {} edited by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
	}

	public void deleteProject(Context ctx) {
		String id = ctx.pathParam("project-id", String.class).get();

		User user = Authenticator.getUser(ctx);
		Session session = ctx.use(Session.class);
		Project project = session.find(Project.class, id);

		if (project == null) {
			throw new NotFoundResponse();
		}

		if (!project.hasPermission(user)) {
			throw new ForbiddenResponse();
		}

		session.delete(project);
		backupAndDelete(getProjectFile(id));

		ctx.status(200);

		logger.info("Project {} deleted by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
	}

	public void uploadProject(Context ctx) throws IOException {
		String id = ctx.pathParam("project-id", String.class).get();
		String projectData = ctx.formParam("project-data", String.class).get();

		if (hasPermission(ctx, id)) {
			Session session = ctx.use(Session.class);

			Project project = session.find(Project.class, id);
			project.setModifiedAt(System.currentTimeMillis());
			session.update(project);

			try (InputStream is = new ByteArrayInputStream(projectData.getBytes(StandardCharsets.UTF_8))) {
				copyInputStreamToFile(is, new File(getProjectFile(id)));
				backup(getProjectFile(id));
			}

			logger.info("Project {} updated by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
		} else {
			throw new UnauthorizedResponse();
		}
	}

	/* Private API */

	private void createProjectJsonFile(String projectId) throws IOException {
		// TODO: Use GSON?
		String dummy = Files.readString(Path.of("Dummy.json"));
		dummy = dummy.replace("<ID>", projectId);
		dummy = dummy.replace("<TIMESTAMP>", String.valueOf(System.currentTimeMillis()));

		InputStream is = new ByteArrayInputStream(dummy.getBytes(StandardCharsets.UTF_8));
		Files.copy(is, Path.of(getProjectFile(projectId)));
	}
}
