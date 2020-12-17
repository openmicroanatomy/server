package fi.ylihallila.server.controllers;

import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.models.Project;
import fi.ylihallila.server.models.Subject;
import fi.ylihallila.server.models.User;
import io.javalin.http.*;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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

		createProjectZipFile(projectId);

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
		project.setSubject(subject);

		session.save(project);

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
			throw new NotFoundResponse();
		}

		FileInputStream fis = new FileInputStream(file);

		ctx.contentType("application/zip").status(200).result(fis.readAllBytes());

		fis.close();
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

		session.createSQLQuery("DELETE FROM WORKSPACES_PROJECTS WHERE PROJECTS_ID = :id")
				.setParameter("id", project.getId())
				.executeUpdate();

		session.delete(project);
		backupAndDelete(getProjectFile(id));

		ctx.status(200);

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
