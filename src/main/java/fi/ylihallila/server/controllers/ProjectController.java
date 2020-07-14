package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Database;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.models.Project;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.models.Workspace;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.UploadedFile;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ProjectController extends BasicController {

	private Logger logger = LoggerFactory.getLogger(ProjectController.class);
//	private Repository<Project> repo;

//	@Inject
//	public ProjectController(@ProjectRepository Repository<Project> repo) {
//		this.repo = repo;
//	}

	public void getAllProjects(Context ctx) {
		Session session = ctx.use(Session.class);

		List<Project> projects = session.createQuery("from Project", Project.class).list();

		ctx.status(200).json(projects);
	}

	public void createPersonalProject(Context ctx) throws IOException {
		String projectName = ctx.formParam("project-name", String.class).get();
		String projectId = UUID.randomUUID().toString();
		Session session = ctx.use(Session.class);
		User user = Authenticator.getUser(ctx);

		Project project = new Project();
		project.setName("Copy of " + projectName);
		project.setId(projectId);
		project.setOwner(user);
		session.save(project);

		createProjectZipFile(projectId);

		ctx.status(200).html(projectId);

		logger.info("Personal project {} created by {}", projectId, user.getName());
	}

	public void createProject(Context ctx) throws IOException {
		String workspaceId = ctx.formParam("workspace-id", String.class).get();
		String projectName = ctx.formParam("project-name", String.class).get();
		String projectId    = UUID.randomUUID().toString();
		Session session = ctx.use(Session.class);
		User user = Authenticator.getUser(ctx);

		session.beginTransaction();

		Project project = new Project();
		project.setId(projectId);
		project.setName(projectName);
		project.setDescription(ctx.formParam("description", ""));

		if (workspaceId.equals("personal")) {
			project.setOwner(user);
		} else {
			project.setOwner(user.getOrganization().getId());
		}

		session.save(project);

		if (!workspaceId.equals("personal")) {
			Workspace workspace = session.find(Workspace.class, workspaceId);
			workspace.addProject(project);
			session.update(workspace);
		}

		createProjectZipFile(projectId);

		logger.info("Project {} created by {}", projectId, user.getName());
	}

	public void downloadProject(Context ctx) throws IOException {
		String id = ctx.pathParam("project-id", String.class).get();

		File file;

		if (ctx.queryParamMap().containsKey("timestamp")) {
			file = new File(getBackupFile(id + ".zip", ctx.queryParam("timestamp")));
		} else {
			file = new File(getProjectFile(id));
		}

		if (!file.exists()) {
			ctx.status(404);
			return;
		}

		InputStream is = new ByteArrayInputStream(new FileInputStream(file).readAllBytes());

		ctx.contentType("application/zip").result(is);

		is.close();
	}

	public void updateProject(Context ctx) {
		String projectId = ctx.pathParam("project-id", String.class).get();

		Session session = ctx.use(Session.class);

		Project project = session.find(Project.class, projectId);
		project.setName(ctx.formParam("name", project.getName()));
		project.setDescription(ctx.formParam("description", project.getDescription()));
		project.setModifiedAt(System.currentTimeMillis());
		session.update(project);

		logger.info("Project {} edited by {}", projectId, Authenticator.getUsername(ctx).orElse("Unknown"));
	}

	public void deleteProject(Context ctx) {
		String id = ctx.pathParam("project-id", String.class).get();

		Session session = ctx.use(Session.class);

		Project project = session.find(Project.class, id);
		session.delete(project);

		delete(getProjectFile(id));

		logger.info("Project {} deleted by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
	}

	public void uploadProject(Context ctx) throws IOException {
		String id = ctx.pathParam("project-id", String.class).get();
		UploadedFile file = ctx.uploadedFile("project");

		if (file == null) {
			logger.info("Tried to upload file but file was missing from form data. [Project: {}, User: {}]", id, Authenticator.getUsername(ctx).orElse("Unknown"));
			ctx.status(400);
			return;
		}

		if (hasPermission(ctx, id)) {
			Session session = ctx.use(Session.class);

			Project project = session.find(Project.class, id);
			project.setModifiedAt(System.currentTimeMillis());
			session.update(project);

			copyInputStreamToFile(file.getContent(), new File(getProjectFile(id)));
			backup(getProjectFile(id));
			logger.info("Project {} updated by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
		} else {
			throw new UnauthorizedResponse();
		}
	}

	/* Private API */

	private void createProjectZipFile(String projectId) throws IOException {
		ZipFile zipFile = new ZipFile(new File("Dummy.zip"));
		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(getProjectFile(projectId)));

		zipFile.stream().forEach(entryIn -> {
			try {
				String name = entryIn.getName().replace("Dummy", projectId);
				ZipEntry newEntry = new ZipEntry(name);
				zipOut.putNextEntry(newEntry);

				InputStream is = zipFile.getInputStream(entryIn);
				int read;
				byte[] bytes = new byte[1024];

				while ((read = is.read(bytes)) != -1) {
					zipOut.write(bytes, 0, read);
				}

				zipOut.closeEntry();
			} catch (IOException e) {
				logger.error("Error while creating a new project zip file", e);
			}
		});

		zipOut.close();
	}
}
