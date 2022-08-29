package fi.ylihallila.server.controllers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.exceptions.UnprocessableEntityResponse;
import fi.ylihallila.server.models.*;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.jackson.FilteredJsonMapper;
import fi.ylihallila.server.util.Util;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.json.JavalinJson;
import io.javalin.plugin.openapi.annotations.*;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
		Allow(ctx, Roles.MODERATOR);

		String workspaceName = ctx.formParam("workspace-name", String.class).get();
		String workspaceId   = UUID.randomUUID().toString();
		Session session      = ctx.use(Session.class);
		User user            = Authenticator.getUser(ctx);

		Workspace workspace = new Workspace();
		workspace.setId(workspaceId);
		workspace.setName(workspaceName);
		workspace.setOwner(user.getOrganization());
		workspace.setSubjects(Collections.emptyList());
		workspace.setWritePermissions(List.of(user));

		session.save(workspace);

		var temp = JavalinJson.getToJsonMapper();
		JavalinJson.setToJsonMapper(new FilteredJsonMapper(user));

		ctx.status(200).json(workspace);

		JavalinJson.setToJsonMapper(temp);

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
		Allow(ctx, Roles.MODERATOR);

		Session session = ctx.use(Session.class);
		User user       = Authenticator.getUser(ctx);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			throw new NotFoundResponse();
		}

		if (!(workspace.getOwner().equals(user.getOrganization()))) {
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
		User user = Authenticator.getUserOrCreateGuestUser(ctx);

		/*
		 * Projects which doesn't have defined any Read permissions is available to everyone, including guests.
		 * If the Read permissions has an organization, only _authenticated_ users belonging to that organization
		 * can view that workspace.
		 */

		// TODO: Remove hidden projects from API

		List<Workspace> workspaces = session.createQuery("from Workspace", Workspace.class).stream()
				.filter(workspace -> !(workspace.getName().contains(Constants.PERSONAL_WORKSPACE_NAME))) // Remove all Personal Workspaces
				.filter(workspace -> workspace.hasReadPermission(user))
				.sorted(Comparator.comparing(Workspace::getName))
				.collect(Collectors.toList());

		if (Authenticator.isLoggedIn(ctx)) {
			workspaces.add(user.getPersonalWorkspace());
		}

		var temp = JavalinJson.getToJsonMapper();
		JavalinJson.setToJsonMapper(new FilteredJsonMapper(user));

		ctx.status(200).json(workspaces);

		JavalinJson.setToJsonMapper(temp);
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
		User user = Authenticator.getUserOrCreateGuestUser(ctx);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			throw new NotFoundResponse();
		}

		if (!(workspace.hasReadPermission(user))) {
			throw new UnauthorizedResponse();
		}

		var temp = JavalinJson.getToJsonMapper();
		JavalinJson.setToJsonMapper(new FilteredJsonMapper(user));

		ctx.status(200).json(workspace);

		JavalinJson.setToJsonMapper(temp);
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
		Allow(ctx, Roles.ANYONE);

		Session session = ctx.use(Session.class);
		User user       = Authenticator.getUser(ctx);

		Workspace workspace = session.find(Workspace.class, id);

		if (workspace == null) {
			throw new NotFoundResponse();
		}

		if (!(workspace.hasWritePermission(user))) {
			throw new ForbiddenResponse();
		}

		if (workspace.getName().contains(Constants.PERSONAL_WORKSPACE_NAME)) {
			throw new UnprocessableEntityResponse("Not allowed to rename personal workspaces");
		}

		workspace.setName(ctx.formParam("workspace-name", workspace.getName()));

		editWritePermissions(session, workspace, ctx);
		editReadPermissions(session, workspace, ctx);

		logger.info("Workspace {} edited by {}", id, user.getName());
	}

	/* Private API */

	/**
	 * Edit write permissions for a workspace. Looks for <code>write</code> form parameter, which should be a JSON
	 * array of UUIDs. Each UUID should point to a {@link fi.ylihallila.server.models.Owner}, which is then set to have
	 * write permissions for the specified <code>workspace</code>.
	 **/
	private void editWritePermissions(Session session, Workspace workspace, Context ctx) {
		if (!(ctx.formParamMap().containsKey("write"))) {
			return;
		}

		try {
			List<String> write = Util.getMapper().readValue(ctx.formParam("write"), new TypeReference<>() {});

			List<Owner> owners = session
					.byMultipleIds(Owner.class)
					.multiLoad(write);

			workspace.setWritePermissions(owners);
		} catch (JacksonException e) {
			logger.error("Error while parsing JSON", e);
		}
	}

	/**
	 * @see WorkspaceController#editReadPermissions(Session, Workspace, Context)
	 */
	private void editReadPermissions(Session session, Workspace workspace, Context ctx) {
		if (!(ctx.formParamMap().containsKey("read"))) {
			return;
		}

		try {
			List<String> read = Util.getMapper().readValue(ctx.formParam("read"), new TypeReference<>() {});

			List<Owner> owners = session
					.byMultipleIds(Owner.class)
					.multiLoad(read);

			workspace.setReadPermissions(owners);
		} catch (JacksonException e) {
			logger.error("Error while parsing JSON", e);
		}
	}
}
