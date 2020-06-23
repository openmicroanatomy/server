package fi.ylihallila.server.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.authentication.Roles;
import fi.ylihallila.server.authentication.impl.TokenAuth;
import fi.ylihallila.server.gson.Error;
import fi.ylihallila.server.gson.Project;
import fi.ylihallila.server.gson.User;
import fi.ylihallila.server.gson.Workspace;
import fi.ylihallila.server.repositories.IRepository;
import fi.ylihallila.server.repositories.Repos;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;

// todo: wip
public class UserController extends BasicController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void hasPermission(Context ctx) {
		String id = ctx.pathParam("id", String.class).get();

		ctx.status(200).json(hasPermission(ctx, id));
	}

	public void getAllUsers(Context ctx) {
		IRepository<User> repo = Repos.getUserRepo();
		ctx.json(repo.list());
	}

	public void getUser(Context ctx) {
		IRepository<User> repo = Repos.getUserRepo();
		Optional<User> query = repo.getById(ctx.pathParam("user-id"));

		if (query.isPresent()) {
			ctx.json(query.get());
		} else {
			ctx.status(404).json(new Error("User not found"));
		}
	}

	public void updateUser(Context ctx) {

	}

	public void login(Context ctx) {
		ctx.json(Authenticator.getUserRoles(ctx));
	}

	/**
	 * This method checks that the given JWT token was issued by Azure AD.
	 * If verified it also returns a list of roles for that user.
	 * @param ctx Context
	 */
	public void verify(Context ctx) {
		DecodedJWT jwt = TokenAuth.validate(ctx);
		String id = jwt.getClaim("oid").asString();

		IRepository<User> userRepo = Repos.getUserRepo();
		Optional<User> query = userRepo.getById(id);

		if (query.isEmpty()) {
			User user = new User();
			user.setName(jwt.getClaim("name").asString());
			user.setId(id);
			user.setRoles(Collections.emptyList());
			user.setOrganizationId(jwt.getClaim("tid").asString());

			userRepo.insert(user);

			ctx.json(Collections.emptyList());
		} else {
			ctx.json(query.get().getRoles());
		}
	}
}
