package fi.ylihallila.server.controllers;

import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.gson.User;
import fi.ylihallila.server.gson.Workspace;
import fi.ylihallila.server.repositories.Repos;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class WorkspaceController extends BasicController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void getAllWorkspaces(Context ctx) throws IOException {
		// Middleware > File Exists
		List<Workspace> workspaces = Repos.getWorkspaceRepo().list();

		if (Authenticator.isLoggedIn(ctx)) {
			User user = Authenticator.getUser(ctx);

			Workspace projects = new Workspace();
			projects.setId((String) null);
			projects.setName("My Projects");
			projects.setOwner(user.getId());
			projects.setProjects(Repos.getProjectRepo().getByOwner(user.getId()));

			workspaces.add(projects);
		}

		ctx.status(200).json(workspaces);
	}

	public void getWorkspace(Context ctx) {
		String id = ctx.pathParam("workspace-id", String.class).get();

		Workspace workspace = Repos.getWorkspaceRepo().getById(id).orElseThrow(NotFoundResponse::new);
		ctx.status(200).json(workspace);
	}

	public void createWorkspace(Context ctx) {
		String workspaceName = ctx.formParam("workspace-name", String.class).get();
		String workspaceId   = UUID.randomUUID().toString();
		User user = Authenticator.getUser(ctx);

		Workspace workspace = new Workspace();
		workspace.setName(workspaceName);
		workspace.setOwner(user.getOrganizationId());
		workspace.setId(workspaceId);
		workspace.setProjects(Collections.emptyList());

		Repos.getWorkspaceRepo().insert(workspace);

		logger.info("Workspace {} created by {}", workspaceId, user.getName());
	}

	public void updateWorkspace(Context ctx) {
		String id = ctx.pathParam("workspace-id", String.class).get();

		Workspace workspace = Repos.getWorkspaceRepo().getById(id).orElseThrow(NotFoundResponse::new);
		workspace.setName(ctx.formParam("workspace-name", workspace.getName()));

		Repos.getWorkspaceRepo().commit();

		logger.info("Workspace {} edited by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
	}

	public void deleteWorkspace(Context ctx) {
		String id = ctx.pathParam("workspace-id", String.class).get();

		Repos.getWorkspaceRepo().deleteById(id);

		logger.info("Workspace {} deleted by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
	}
}
