package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Database;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.models.*;
import io.javalin.http.Context;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WorkspaceController extends BasicController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void getAllWorkspaces(Context ctx) {
		Session session = ctx.use(Session.class);

		List<Workspace> workspaces = session.createQuery("from Workspace", Workspace.class).list();

		if (Authenticator.isLoggedIn(ctx) && Authenticator.hasPermissions(ctx, Roles.MANAGE_PERSONAL_PROJECTS)) {
			User user = Authenticator.getUser(ctx);

			Workspace personal = new Workspace();
			personal.setId((String) null);
			personal.setName("My Projects");
			personal.setOwner(user);

			List<Project> projects = session.createQuery("from Project where owner.id = :id", Project.class)
					                        .setParameter("id", user.getId()).list();
			personal.setProjects(projects);

			workspaces.add(personal);
		}

		ctx.status(200).json(workspaces);
	}

	public void getWorkspace(Context ctx) {
		String id = ctx.pathParam("workspace-id", String.class).get();
		Session session = ctx.use(Session.class);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			ctx.status(404); return;
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
		workspace.setProjects(Collections.emptyList());
		session.save(workspace);

		logger.info("Workspace {} created by {}", workspaceId, user.getName());
	}

	public void updateWorkspace(Context ctx) {
		String id = ctx.pathParam("workspace-id", String.class).get();
		Session session = ctx.use(Session.class);
		User user = Authenticator.getUser(ctx);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			ctx.status(404); return;
		}

		if (!workspace.hasPermission(user)) {
			ctx.status(403); return;
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
			ctx.status(404); return;
		}

		if (!workspace.hasPermission(user)) {
			ctx.status(403); return;
		}

		session.delete(workspace);

		logger.info("Workspace {} deleted by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
	}
}
