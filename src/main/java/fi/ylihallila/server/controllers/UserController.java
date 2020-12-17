package fi.ylihallila.server.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.authentication.impl.TokenAuth;
import fi.ylihallila.server.models.Error;
import fi.ylihallila.server.models.Organization;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.PasswordHelper;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static fi.ylihallila.server.util.Config.Config;

public class UserController extends Controller {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void hasPermission(Context ctx) {
		String id = ctx.pathParam("id", String.class).get();

		ctx.status(200).json(hasPermission(ctx, id));
	}

	public void getAllUsers(Context ctx) {
		User user = Authenticator.getUser(ctx);
		Session session = ctx.use(Session.class);

		List<User> users;

		if (user.hasRole(Roles.ADMIN)) {
			users = session.createQuery("from User", User.class).list();
		} else {
			users = session.createQuery("from User where organization.id = :id")
					.setParameter("id", user.getOrganization().getId()).list();
		}

		ctx.status(200).json(users);
	}

	public void getUser(Context ctx) {
		Session session = ctx.use(Session.class);

		User user = session.find(User.class, ctx.pathParam("user-id"));

		if (user != null) {
			ctx.status(200).json(user);
		} else {
			ctx.status(404).json(new Error("User not found"));
		}
	}

	public void createUser(Context ctx) {
		String organizationId = ctx.formParam("organization-id", String.class).get();
		String password		  = ctx.formParam("password", String.class).get();
		String email		  = ctx.formParam("email", String.class).get();
		String name		      = ctx.formParam("name", String.class).get();

		Session session = ctx.use(Session.class);

		Organization organization = session.find(Organization.class, organizationId);
		if (organization == null) {
			throw new NotFoundResponse("Organization not found.");
		}

		User user = new User();
		user.setOrganization(organization);
		user.setName(name);
		user.hashPassword(password);
		user.setEmail(email);

		session.save(user);

		ctx.status(201).json(user);
	}

	public void updateUser(Context ctx) {
		String id = ctx.pathParam("user-id");

		Session session = ctx.use(Session.class);
		User user = Authenticator.getUser(ctx);

		User editedUser = session.find(User.class, id);

		if (editedUser == null) {
			throw new NotFoundResponse("User not found");
		}

		Set<Roles> roles = editedUser.getRoles();

		for (Roles role : Roles.getModifiableRoles()) {
			if (ctx.formParamMap().containsKey(role.name())) {
				boolean addRole = ctx.formParam(role.name(), Boolean.class).get();

				if (addRole) {
					roles.add(role);
				} else {
					roles.remove(role);
				}
			}
		}

		if (user.hasRole(Roles.ADMIN)) {
			if (ctx.formParamMap().containsKey("password")) {
				// TODO: Password security requirements
				editedUser.hashPassword(ctx.formParam("password"));
			}
		}

		// TODO: Add validators
		editedUser.setName(ctx.formParam("name", editedUser.getName()));
		editedUser.setEmail(ctx.formParam("email", editedUser.getEmail()));
		editedUser.setRoles(roles);
		session.update(editedUser);

		ctx.status(200);

		// TODO: Remove `password` field from formParamMap
		logger.info("User {} was edited by {} [{}]", editedUser.getName(), user.getName(), ctx.formParamMap());
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
		data.put("organizationId", user.getOrganization().getId());
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

		Session session = ctx.use(Session.class);

		User user = session.find(User.class, id);

		if (user == null) {
			user = new User();
			user.setId(id);
			user.setName(jwt.getClaim("name").asString());
			user.setEmail(jwt.getClaim("email").asString());
			user.setOrganization(jwt.getClaim("tid").asString());

			if (Config.getBoolean("roles.manage.personal.projects.default")) {
				user.setRoles(Set.of(Roles.MANAGE_PERSONAL_PROJECTS));
			}

			session.save(user);
		}

		ctx.status(200).json(user.getRoles());
	}
}
