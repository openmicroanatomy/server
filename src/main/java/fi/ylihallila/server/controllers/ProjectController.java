package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Config;
import fi.ylihallila.server.gson.Project;
import fi.ylihallila.server.gson.Workspace;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ProjectController extends BasicController {

	private Logger logger = LoggerFactory.getLogger(ProjectController.class);

	public void createProject(Context ctx) throws IOException {
		String targetWorkspace = ctx.formParam("workspace-id", String.class).get();
		String projectName = ctx.formParam("project-name", String.class).get();
		String projectId = UUID.randomUUID().toString();

		List<Workspace> workspaces = getWorkspaces();

		Project project = new Project();
		project.setName(projectName);
		project.setId(projectId);
		project.setDescription(ctx.formParam("description", ""));
		project.setThumbnail("");
		project.setServer("");

		AtomicBoolean success = new AtomicBoolean(false);

		workspaces.forEach(workspace -> {
			if (workspace.getId().equalsIgnoreCase(targetWorkspace)) {
				success.set(true);
				workspace.addProject(project);
			}
		});

		if (success.get()) {
			createProjectZipFile(projectId);
			backup(getProjectFile(projectId));

			saveAndBackup(Path.of(Config.WORKSPACE_FILE), workspaces);
			ctx.status(200);
		} else {
			ctx.status(404);
		}
	}

	public void downloadProject(Context ctx) throws IOException {
		String projectId = ctx.pathParam("project-id", String.class).get();
		File file = new File(getProjectFile(projectId));

		if (!file.exists()) {
			ctx.status(404);
			return;
		}

		InputStream is = new ByteArrayInputStream(new FileInputStream(file).readAllBytes());

		ctx.contentType("application/zip");
		ctx.result(is);

		is.close();
	}

	public void updateProject(Context ctx) throws IOException {
		String projectId = ctx.pathParam("project-id", String.class).get();

		List<Workspace> workspaces = getWorkspaces();
		workspaces.forEach(workspace -> {
			workspace.getProjects().forEach(project -> {
				if (project.getId().equalsIgnoreCase(projectId)) {
					project.setName(ctx.formParam("name", project.getName()));
					project.setDescription(ctx.formParam("description", project.getDescription()));
				}
			});
		});

		saveAndBackup(Path.of(Config.WORKSPACE_FILE), workspaces);
	}

	public void deleteProject(Context ctx) throws IOException {
		String projectToDelete = ctx.pathParam("project-id", String.class).get();

		List<Workspace> workspaces = getWorkspaces();
		workspaces.forEach(workspace -> {
			workspace.getProjects().removeIf(project ->
				project.getId().equalsIgnoreCase(projectToDelete)
			);
		});

		delete(getProjectFile(projectToDelete));
		saveAndBackup(Path.of(Config.WORKSPACE_FILE), workspaces);
		ctx.status(200);
	}

	public void uploadProject(Context ctx) throws IOException {
		String projectId = ctx.pathParam("project-id", String.class).get();
		UploadedFile file = ctx.uploadedFile("project");

		if (file == null) {
			logger.info("Tried to upload file, but didn't exist as form data");
			ctx.status(400);
			return;
		}

		copyInputStreamToFile(file.getContent(), new File(getProjectFile(projectId)));
		backup(getProjectFile(projectId));
		ctx.status(200);
	}

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
