package fi.ylihallila.server.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.authentication.impl.TokenAuth;
import fi.ylihallila.server.models.Error;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.repositories.Repository;
import fi.ylihallila.server.repositories.Repos;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static fi.ylihallila.server.util.Config.Config;

public class UserController extends BasicController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void hasPermission(Context ctx) {
		String id = ctx.pathParam("id", String.class).get();

		ctx.status(200).json(hasPermission(ctx, id));
	}

	public void getAllUsers(Context ctx) {
		Repository<User> repo = Repos.getUserRepo();
		ctx.json(repo.list());
	}

	public void getUser(Context ctx) {
		Repository<User> repo = Repos.getUserRepo();
		Optional<User> query = repo.getById(ctx.pathParam("user-id"));

		if (query.isPresent()) {
			ctx.json(query.get());
		} else {
			ctx.status(404).json(new Error("User not found"));
		}
	}

	public void updateUser(Context ctx) {
		Repository<User> repo = Repos.getUserRepo();
		String id = ctx.pathParam("user-id");
		User user = repo.getById(id).orElseThrow(NotFoundResponse::new);

		Set<Roles> roles = user.getRoles();

		var modified = false;
		for (Roles role : Roles.getModifiableRoles()) {
			if (ctx.formParamMap().containsKey(role.name())) {
				var addRole = ctx.formParam(role.name(), Boolean.class).get();

				if (addRole) {
					roles.add(role);
				} else {
					roles.remove(role);
				}

				modified = true;
			}
		}

		if (modified) {
			user.setRoles(roles);
			repo.commit();
			ctx.status(200);

			logger.info("Roles of {} were edited by {}. New values: {}", id, Authenticator.getUsername(ctx).orElse("Unknown"), ctx.formParamMap());
		} else {
			ctx.status(400);
			logger.debug("{} tried to edit user {} but formParams were incorrect: {}", Authenticator.getUsername(ctx).orElse("Unknown"), id, ctx.formParamMap());
		}
	}

	/**
	 * This method is used when authenticating with Basic Authentication. The endpoint returns usedId,
	 * organizationId and roles. If the credentials are invalid, an NotAuthorizedException is thrown.

	 * @param ctx Context
	 */
	public void login(Context ctx) {
		User user = Authenticator.getUser(ctx);

		Map<String, Object> data = new HashMap<>();

		data.put("userId", user.getId());
		data.put("organizationId", user.getOrganizationId());
		data.put("roles", user.getRoles());

		ctx.status(200).json(data);
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

		Repository<User> repo = Repos.getUserRepo();
		Optional<User> query = repo.getById(id);

		if (query.isEmpty()) {
			User user = new User();
			user.setId(id);
			user.setName(jwt.getClaim("name").asString());
			user.setEmail(jwt.getClaim("email").asString());
			user.setOrganizationId(jwt.getClaim("tid").asString());

			if (Config.getBoolean("roles.manage.personal.projects.default")) {
				user.setRoles(Set.of(Roles.MANAGE_PERSONAL_PROJECTS));
			}

			repo.insert(user);

			ctx.json(user.getRoles());
		} else {
			ctx.json(query.get().getRoles());
		}
	}
}
