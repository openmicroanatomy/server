package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Config;
import fi.ylihallila.server.authentication.Auth;
import fi.ylihallila.server.gson.Workspace;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorkspaceController extends BasicController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

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

		saveAndBackup(Path.of(Config.WORKSPACE_FILE), workspaces);
	}

	public void updateWorkspace(Context ctx) throws IOException {
		String workspaceId = ctx.pathParam("workspace-id", String.class).get();

		ArrayList<Workspace> workspaces = getWorkspaces();
		Workspace workspace = getWorkspace(workspaces, workspaceId);
		workspace.setName(ctx.formParam("workspace-name", workspace.getName()));

		logger.info("Workspace {} edited by {}", workspaceId, Auth.getUsername(ctx).orElse("Unknown"));
		saveAndBackup(Path.of(Config.WORKSPACE_FILE), workspaces);

		ctx.status(200);
	}

	public void deleteWorkspace(Context ctx) throws IOException {
		String workspaceToDelete = ctx.pathParam("workspace-id", String.class).get();

		ArrayList<Workspace> workspaces = getWorkspaces();
		workspaces.remove(getWorkspace(workspaces, workspaceToDelete));

//		workspaces.removeIf(workspace ->
//			workspace.getId().equalsIgnoreCase(workspaceToDelete)
//		);

//		if (deleted) {
			saveAndBackup(Path.of(Config.WORKSPACE_FILE), workspaces);
			ctx.status(200);
//		} else {
//			ctx.status(404);
//		}
	}

	private Workspace getWorkspace(ArrayList<Workspace> workspaces, String workspaceId) {
		List<Workspace> list = workspaces.stream().filter(
			workspace -> workspace.getId().equalsIgnoreCase(workspaceId)
		).limit(2).collect(Collectors.toList());

		if (list.size() == 1) {
			return list.get(0);
		}

		throw new NotFoundResponse("Cannot find workspace with UUID=" + workspaceId);
	}
}
