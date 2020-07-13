package fi.ylihallila.server.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fi.ylihallila.server.Util;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.models.Workspace;
import fi.ylihallila.server.models.WorkspaceExpanded;
import fi.ylihallila.server.repositories.Repos;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WorkspaceController extends BasicController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void getAllWorkspaces(Context ctx) throws JsonProcessingException {
		List<Workspace> workspaces = Repos.getWorkspaceRepo().list();

		if (ctx.queryParamMap().containsKey("owner")) {
			workspaces.removeIf(workspace -> !workspace.getOwner().equalsIgnoreCase(ctx.queryParam("owner")));
		}

		if (Authenticator.isLoggedIn(ctx) && Authenticator.hasPermissions(ctx, Roles.MANAGE_PERSONAL_PROJECTS)) {
			User user = Authenticator.getUser(ctx);

			Workspace projects = new Workspace();
			projects.setId((String) null);
			projects.setName("My Projects");
			projects.setOwner(user.getId());
			projects.setProjects(Repos.getProjectRepo().getByOwner(user.getId()));

			workspaces.add(projects);
		}

		ObjectMapper temp = Util.getMapper().copy();
		SimpleModule simpleModule = new SimpleModule();
		simpleModule.setMixInAnnotation(Workspace.class, WorkspaceExpanded.class);
		temp.registerModule(simpleModule);

		ctx.status(200).contentType("application/json").result(temp.writeValueAsString(workspaces));
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
