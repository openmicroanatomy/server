package fi.ylihallila.server.controllers;

import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.models.*;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WorkspaceController extends Controller {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void getAllWorkspaces(Context ctx) {
		Session session = ctx.use(Session.class);

		// TODO: Remove hidden projects from API for users without write access
		List<Workspace> workspaces = session.createQuery("from Workspace", Workspace.class).list();

		// TODO: Reimplement personal projects

		ctx.status(200).json(workspaces);
	}

	public void getWorkspace(Context ctx) {
		String id = ctx.pathParam("workspace-id", String.class).get();
		Session session = ctx.use(Session.class);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			throw new NotFoundResponse();
		}

		ctx.status(200).json(workspace);
	}

	public void createWorkspace(Context ctx) {
		String workspaceName = ctx.formParam("workspace-name", String.class).get();
		String workspaceId   = UUID.randomUUID().toString();
		Session session = ctx.use(Session.class);
		User user = Authenticator.getUser(ctx);

		Workspace workspace = new Workspace();
		workspace.setId(workspaceId);
		workspace.setName(workspaceName);
		workspace.setOwner(user.getOrganization());
		workspace.setSubjects(Collections.emptyList());
		session.save(workspace);

		logger.info("Workspace {} created by {}", workspaceId, user.getName());
	}

	public void updateWorkspace(Context ctx) {
		String id = ctx.pathParam("workspace-id", String.class).get();
		Session session = ctx.use(Session.class);
		User user = Authenticator.getUser(ctx);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			throw new NotFoundResponse();
		}

		if (!workspace.hasPermission(user)) {
			throw new ForbiddenResponse();
		}

		workspace.setName(ctx.formParam("workspace-name", workspace.getName()));

		logger.info("Workspace {} edited by {}", id, user.getName());
	}

	public void deleteWorkspace(Context ctx) {
		String id = ctx.pathParam("workspace-id", String.class).get();
		Session session = ctx.use(Session.class);
		User user = Authenticator.getUser(ctx);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			throw new NotFoundResponse();
		}

		if (!workspace.hasPermission(user)) {
			throw new ForbiddenResponse();
		}

		session.delete(workspace);

		logger.info("Workspace {} deleted by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
	}
}
