package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Configuration;
import fi.ylihallila.server.gson.Workspace;
import io.javalin.http.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class WorkspaceController extends BasicController {

	public void getWorkspaces(Context ctx) throws IOException {
		// Middleware > File Exists
		ctx.result(Files.readString(getWorkspaceFile()));
		ctx.status(200).contentType("application/json");
	}

	public void createWorkspace(Context ctx) throws IOException {
		String workspaceName = ctx.formParam("workspace-name", String.class).get();
		List<Workspace> workspaces = getWorkspaces();

		Workspace workspace = new Workspace();
		workspace.setName(workspaceName);
		workspace.setId(UUID.randomUUID());
		workspace.setProjects(Collections.emptyList());

		workspaces.add(workspace);

		saveAndBackup(Path.of(Configuration.WORKSPACE_FILE), workspaces);
	}

	public void deleteWorkspace(Context ctx) throws IOException {
		String workspaceToDelete = ctx.pathParam("workspace-id", String.class).get();

		List<Workspace> workspaces = getWorkspaces();
		var deleted = workspaces.removeIf(workspace ->
			workspace.getId().equalsIgnoreCase(workspaceToDelete)
		);

		if (deleted) {
			saveAndBackup(Path.of(Configuration.WORKSPACE_FILE), workspaces);
			ctx.status(200);
		} else {
			ctx.status(404);
		}
	}
}
