package fi.ylihallila.server.controllers;

import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.exceptions.UnprocessableEntityResponse;
import fi.ylihallila.server.models.*;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.openapi.annotations.*;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

// TODO: Add support for personal workspaces
public class WorkspaceController extends Controller implements CrudHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@OpenApi(
		tags = { "workspaces" },
		summary = "Create a new workspace",
		formParams = {
			@OpenApiFormParam(name = "workspace-name", required = true)
		},
		responses = {
			@OpenApiResponse(status = "200")
		}
	)
	@Override public void create(@NotNull Context ctx) {
		Allow(ctx, Roles.MANAGE_PROJECTS);

		String workspaceName = ctx.formParam("workspace-name", String.class).get();
		String workspaceId   = UUID.randomUUID().toString();
		Session session      = ctx.use(Session.class);
		User user            = Authenticator.getUser(ctx);

		Workspace workspace = new Workspace();
		workspace.setId(workspaceId);
		workspace.setName(workspaceName);
		workspace.setOwner(user.getOrganization());
		workspace.setSubjects(Collections.emptyList());

		session.save(workspace);

		ctx.status(200).json(workspace);

		logger.info("Workspace {} created by {}", workspaceId, user.getName());
	}

	@OpenApi(
		tags = { "workspaces" },
		summary = "Delete given workspace",
		pathParams = {
			@OpenApiParam(name = "id", required = true),
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404"),
		}
	)
	@Override public void delete(@NotNull Context ctx, @NotNull String id) {
		Allow(ctx, Roles.MANAGE_PROJECTS);

		Session session = ctx.use(Session.class);
		User user       = Authenticator.getUser(ctx);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			throw new NotFoundResponse();
		}

		if (!(workspace.hasPermission(user))) {
			throw new ForbiddenResponse();
		}

		session.delete(workspace);

		logger.info("Workspace {} deleted by {}", id, Authenticator.getUsername(ctx).orElse("Unknown"));
	}

	@OpenApi(
		tags = { "workspaces" },
		summary = "Get all workspaces",
		responses = {
			@OpenApiResponse(status = "200", content = @OpenApiContent(from = Workspace.class, isArray = true)),
		}
	)
	@Override public void getAll(@NotNull Context ctx) {
		Session session = ctx.use(Session.class);

		// TODO: Remove hidden projects from API for users without write access
		List<Workspace> workspaces = session.createQuery("from Workspace", Workspace.class).list();

		// TODO: Reimplement personal projects

		ctx.status(200).json(workspaces);
	}

	@OpenApi(
		tags = { "workspaces" },
		summary = "Fetch given workspace",
		pathParams = {
			@OpenApiParam(name = "id", required = true),
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "404")
		}
	)
	@Override public void getOne(@NotNull Context ctx, @NotNull String id) {
		Session session = ctx.use(Session.class);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			throw new NotFoundResponse();
		}

		ctx.status(200).json(workspace);
	}

	@OpenApi(
		tags = { "workspaces" },
		summary = "Update given workspace",
		pathParams = {
			@OpenApiParam(name = "id", required = true),
		},
		formParams = {
			@OpenApiFormParam(name = "workspace-name")
		},
		responses = {
			@OpenApiResponse(status = "200"),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404"),
			@OpenApiResponse(status = "422"),
		}
	)
	@Override public void update(@NotNull Context ctx, @NotNull String id) {
		Allow(ctx, Roles.MANAGE_PROJECTS);

		Session session = ctx.use(Session.class);
		User user       = Authenticator.getUser(ctx);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			throw new NotFoundResponse();
		}

		if (!workspace.hasPermission(user)) {
			throw new ForbiddenResponse();
		}

		if (workspace.getName().contains("Personal Workspace")) {
			throw new UnprocessableEntityResponse("Not allowed to rename personal workspaces");
		}

		workspace.setName(ctx.formParam("workspace-name", workspace.getName()));

		logger.info("Workspace {} edited by {}", id, user.getName());
	}
}
