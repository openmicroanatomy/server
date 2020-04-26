package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Configuration;
import fi.ylihallila.server.gson.Project;
import fi.ylihallila.server.gson.Workspace;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ProjectController extends BasicController {

	private Logger logger = LoggerFactory.getLogger(ProjectController.class);

	public void createProject(Context ctx) throws IOException {
		String targetWorkspace = ctx.formParam("workspace-name", String.class).get();
		String projectName = ctx.formParam("project-name", String.class).get();

		List<Workspace> workspaces = getWorkspaces();

		Project project = new Project();
		project.setName(projectName);
		project.setId(projectName); 	// TODO: Support UUIDs to allow same names
		project.setDescription(ctx.formParam("description", ""));
		project.setThumbnail("");
		project.setServer("");

		workspaces.forEach(workspace -> {
			if (workspace.getName().equalsIgnoreCase(targetWorkspace)) {
				workspace.addProject(project);
			}
		});

		createProjectZipFile(projectName);
		backup(getProjectFile(projectName));
		saveWorkspace(workspaces);
	}

	public void downloadProject(Context ctx) throws IOException {
		File file = new File(getProjectFile(ctx.pathParam("project-name")));

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
		String targetProject = ctx.pathParam("project-name", String.class).get();

		List<Workspace> workspaces = getWorkspaces();
		workspaces.forEach(workspace -> {
			workspace.getProjects().forEach(project -> {
				if (project.getName().equalsIgnoreCase(targetProject)) {
					project.setName(ctx.formParam("name", project.getName()));
					project.setDescription(ctx.formParam("description", project.getDescription()));
				}
			});
		});

		saveWorkspace(workspaces);
		backup(Configuration.WORKSPACE_FILE);
	}

	public void deleteProject(Context ctx) throws IOException {
		String projectToDelete = ctx.pathParam("project-name", String.class).get();

		List<Workspace> workspaces = getWorkspaces();
		workspaces.forEach(workspace -> {
			workspace.getProjects().removeIf(project ->
				project.getName().equalsIgnoreCase(projectToDelete)
			);
		});

		delete(getProjectFile(projectToDelete));
		saveWorkspace(workspaces);
		ctx.status(200);
	}

	public void uploadProject(Context ctx) throws IOException {
		String projectName = ctx.pathParam("project-name", String.class).get();
		UploadedFile file = ctx.uploadedFile("project");

		if (file == null) {
			logger.debug("Tried to upload file, but didn't exist as form data");
			ctx.status(400);
			return;
		}

		copyInputStreamToFile(file.getContent(), new File(getProjectFile(projectName)));
		backup(getProjectFile(projectName));
		ctx.status(200);
	}

	private void createProjectZipFile(String projectName) throws IOException {
		ZipFile zipFile = new ZipFile(new File("Dummy.zip"));
		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(getProjectFile(projectName)));

		zipFile.stream().forEach(entryIn -> {
			try {
				String name = entryIn.getName().replace("Dummy", projectName);
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
