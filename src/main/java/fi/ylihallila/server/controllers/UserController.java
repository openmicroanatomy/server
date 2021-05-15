package fi.ylihallila.server.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.authentication.impl.TokenAuth;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.models.Organization;
import fi.ylihallila.server.models.User;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static fi.ylihallila.server.util.Config.Config;

public class UserController extends Controller implements CrudHandler {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@OpenApi(
		summary = "Get all users",
		tags = { "user" },
		responses = {
			@OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class, isArray = true)),
			@OpenApiResponse(status = "403")
		}
	)
	@Override public void getAll(@NotNull Context ctx) {
		Allow(ctx, Roles.MANAGE_USERS);

		Session session = ctx.use(Session.class);
		User user       = Authenticator.getUser(ctx);

		List<User> users;

		if (user.hasRole(Roles.ADMIN)) {
			users = session.createQuery("from User", User.class).list();
		} else {
			users = session.createQuery("from User where organization.id = :id")
					.setParameter("id", user.getOrganization().getId()).list();
		}

		ctx.status(200).json(users);
	}

	@OpenApi(
		summary = "Get given user",
		tags = { "user" },
		responses = {
			@OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404")
		}
	)
	@Override public void getOne(@NotNull Context ctx, @NotNull String id) {
		Allow(ctx, Roles.MANAGE_USERS);

		User queriedUser = getUser(ctx, id);
		User user        = Authenticator.getUser(ctx);

		if (user.hasRole(Roles.ADMIN) || hasSameOrganization(user, queriedUser)) {
			ctx.status(200).json(queriedUser);
		} else {
			throw new UnauthorizedResponse("Not authorized to view this user.");
		}
	}

	@OpenApi(
		summary = "Create a new user",
		tags = { "user" },
		responses = {
			@OpenApiResponse(status = "201", content = @OpenApiContent(from = User.class)),
			@OpenApiResponse(status = "400"),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404")
		}
	)
	@Override public void create(@NotNull Context ctx) {
		Allow(ctx, Roles.MANAGE_USERS);

		String password	= ctx.formParam("password", String.class).get();
		String email	= ctx.formParam("email", String.class).get();
		String name		= ctx.formParam("name", String.class).get();

		Session session = ctx.use(Session.class);
		User user = Authenticator.getUser(ctx);

		User newUser = new User();
		newUser.setId(UUID.randomUUID());
		newUser.setName(name);
		newUser.setEmail(email);
		newUser.hashPassword(password);

		if (user.hasRole(Roles.ADMIN) && ctx.formParamMap().containsKey("organization")) {
			Organization organization = session.find(Organization.class, ctx.formParam("organization", String.class).get());

			if (organization != null) {
				newUser.setOrganization(organization);
			} else {
				throw new NotFoundResponse("Could not find provided organization.");
			}
		} else {
			newUser.setOrganization(user.getOrganization());
		}

		if (Config.getBoolean("roles.manage.personal.projects.default")) {
			newUser.setRoles(EnumSet.of(Roles.MANAGE_PERSONAL_PROJECTS));
		} else {
			newUser.setRoles(EnumSet.noneOf(Roles.class));
		}

		session.save(newUser);

		ctx.status(201).json(newUser);

		logger.info("User {} created by {} [Organization: {}]", newUser.getName(), user.getName(), user.getOrganization().getName());
	}

	@OpenApi(
		summary = "Update given user",
		tags = { "user" },
		pathParams = @OpenApiParam(
			name = "id",
			description = "UUID of user to be updated",
			required = true
		),
		responses = {
			@OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
			@OpenApiResponse(status = "403"),
			@OpenApiResponse(status = "404")
		}
	)
	@Override public void update(@NotNull Context ctx, @NotNull String id) {
		Allow(ctx, Roles.ANYONE);

		Session session = ctx.use(Session.class);
		User editedUser = getUser(ctx, id);
		User user       = Authenticator.getUser(ctx);

		if (editedUser.equals(user)) {
			// If the user has MANAGE_USERS role, they can edit their own roles. Otherwise editing is restricted to personal details

			editPersonalDetails(user, editedUser, ctx);

			if (user.hasRole(Roles.MANAGE_USERS)) {
				editRoles(user, editedUser, ctx);
			}
		} else if (user.hasRole(Roles.MANAGE_USERS)) {
			// User can be edited if the user belongs to the same organization or the editing user has the role ADMIN

			if (hasSameOrganization(user, editedUser) || user.hasRole(Roles.ADMIN)) {
				editPersonalDetails(user, editedUser, ctx);
				editRoles(user, editedUser, ctx);
			} else {
				throw new UnauthorizedResponse("Not authorized to edit that user.");
			}
		} else {
			throw new UnauthorizedResponse("Not authorized to edit that user.");
		}

		session.update(editedUser);

		ctx.status(200).json(user);

		var cleanedFormParamMap = ctx.formParamMap();
		cleanedFormParamMap.remove("password");

		logger.info("User {} was edited by {} [{}]", editedUser.getName(), user.getName(), cleanedFormParamMap);
	}

	@OpenApi(
		summary = "Delete given user",
		tags = { "user" },
		pathParams = @OpenApiParam(
			name = "id",
			description = "UUID of user to be deleted",
			required = true
		)
	)
	@Override public void delete(@NotNull Context ctx, @NotNull String id) {
		Allow(ctx, Roles.MANAGE_USERS);

		Session session  = ctx.use(Session.class);
		User deletedUser = getUser(ctx, id);
		User user        = Authenticator.getUser(ctx);

		if (user.hasRole(Roles.ADMIN) || hasSameOrganization(user, deletedUser)) {
			session.delete(deletedUser);
			ctx.status(200);

			logger.info("User {} [{}] deleted by {} [{}]", deletedUser.getName(), deletedUser.getId(), user.getName(), user.getId());
		} else {
			logger.warn("User {} [{}] tried to delete user {} [{}] but lacked permissions.", user.getName(), user.getId(), deletedUser.getName(), deletedUser.getId());

			throw new UnauthorizedResponse("No permission to delete this user");
		}
	}

	public void hasPermission(Context ctx) {
		String id = ctx.pathParam("id", String.class).get();

		ctx.status(200).json(hasPermission(ctx, id));
	}

	/**
	 * This method is used when authenticating with Basic Authentication. The endpoint returns an User object.
	 * If the credentials are invalid, an NotAuthorizedException is thrown.

	 * @param ctx Context
	 */
	public void login(Context ctx) {
		User user = Authenticator.getUser(ctx);

		ctx.status(200).json(user);
	}

	/**
	 * This method checks that the given JWT token was issued by Azure AD and is valid.
	 *
	 * If verified, returns a list of roles for that user. This endpoint *does not* return
	 * the userId and organizationId, as these are encoded within the JWT itself.
	 *
	 * @param ctx Context
	 */
	public void verify(Context ctx) {
		DecodedJWT jwt = TokenAuth.validate(ctx);
		String id = jwt.getClaim("oid").asString();

		Session session = ctx.use(Session.class);
		User user = session.find(User.class, id);

		if (user == null) {
			user = new User();
			user.setId(id);
			user.setName(jwt.getClaim("name").asString());
			user.setEmail(jwt.getClaim("email").asString());
			user.setOrganization(jwt.getClaim("tid").asString());

			if (Config.getBoolean("roles.manage.personal.projects.default")) {
				user.setRoles(EnumSet.of(Roles.MANAGE_PERSONAL_PROJECTS));
			}

			session.save(user);
		}

		ctx.status(200).json(user);
	}

	/* PRIVATE API */

	private User getUser(Context ctx, String id) {
		Session session = ctx.use(Session.class);
		User user = session.find(User.class, id);

		if (user == null) {
			throw new NotFoundResponse("User not found");
		}

		return user;
	}

	private boolean hasSameOrganization(User user, User otherUser) {
		return user.getOrganization().equals(otherUser.getOrganization());
	}

	/**
	 * Personal details include username, email and password.
	 * Passwords can be changed by only the user themselves or by an user with the ADMIN role.
	 */
	private void editPersonalDetails(User user, User editedUser, Context ctx) {
		if (user.equals(editedUser) || user.hasRole(Roles.ADMIN)) {
			if (ctx.formParamMap().containsKey("password")) {
				String password = ctx.formParam("password", String.class).get();

				editedUser.hashPassword(password);
			}
		}

		editedUser.setName(ctx.formParam("name", editedUser.getName()));
		editedUser.setEmail(ctx.formParam("email", editedUser.getEmail()));
	}

	private void editRoles(User user, User editedUser, Context ctx) {
		EnumSet<Roles> roles = editedUser.getRoles();

		for (Roles role : Roles.getModifiableRoles()) {
			if (ctx.formParamMap().containsKey(role.name())) {
				if (role == Roles.ADMIN && !(user.hasRole(Roles.ADMIN))) {
					logger.info("User {} [{}] tried to edit administrative roles for {} [{}] but lacked permissions", user.getName(), user.getId(), editedUser.getName(), editedUser.getId());
					continue;
				}

				boolean addRole = ctx.formParam(role.name(), Boolean.class).get();

				if (addRole) {
					roles.add(role);
				} else {
					roles.remove(role);
				}
			}
		}

		editedUser.setRoles(roles);
	}
}
