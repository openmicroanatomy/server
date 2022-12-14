package fi.ylihallila.server.authentication;

import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.authentication.impl.BasicAuth;
import fi.ylihallila.server.authentication.impl.TokenAuth;
import fi.ylihallila.server.exceptions.UnprocessableEntityResponse;
import fi.ylihallila.server.models.Error;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.Guest;
import io.javalin.core.security.Role;
import io.javalin.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Authenticator {

	private static final Logger logger = LoggerFactory.getLogger(Authenticator.class);

	private final static BasicAuth basicAuth = new BasicAuth();
	private final static TokenAuth tokenAuth = new TokenAuth();

	public static void accessManager(Handler handler, Context ctx, Set<Role> permittedRoles) {
		try {
			if (permittedRoles.isEmpty() || permittedRoles.contains(Roles.ANYONE) || hasRoles(ctx, permittedRoles)) {
				handler.handle(ctx);
			} else {
				if (isLoggedIn(ctx)) {
					ctx.status(403).json(new Error("Unauthorized. User lacks required permissions to access this resource."));
				} else {
					ctx.status(401).json(new Error("Forbidden. Use Basic authentication or provide `Token` header."));
				}
			}
		} catch (UnprocessableEntityResponse e) {
			ctx.status(422).json(new Error(e.getLocalizedMessage()));
		} catch (NotFoundResponse e) {
			ctx.status(404).json(new Error(e.getLocalizedMessage()));
		} catch (ForbiddenResponse e) {
			ctx.status(403).json(new Error(e.getLocalizedMessage()));
		} catch (UnauthorizedResponse e) {
			ctx.status(401).json(new Error(e.getLocalizedMessage()));
		} catch (BadRequestResponse e) {
			ctx.status(400).json(new Error(e.getLocalizedMessage()));
		} catch (InternalServerErrorResponse e) {
			ctx.status(500).json(new Error(e.getLocalizedMessage()));
		} catch (Exception e) {
			ctx.status(500).json(new Error("Internal server error"));
			logger.error("Unknown exception", e);
		}
	}

	public static boolean isLoggedIn(Context ctx) {
		return getAuthImpl(ctx).isLoggedIn(ctx);
	}

	public static boolean hasRoles(Context ctx, Role permittedRole) {
		return hasRoles(ctx, Set.of(permittedRole));
	}

	public static boolean hasRoles(Context ctx, Set<Role> permittedRoles) {
		return getAuthImpl(ctx).hasRoles(ctx, permittedRoles);
	}

	/**
	 * Checks that the user is logged in and then returns the user object.
	 * @param ctx Context
	 * @return User object, null if not found or not logged in or invalid credentials
	 */
	public static User getUser(Context ctx) {
		return getAuthImpl(ctx).getUser(ctx);
	}

	/**
	 * Returns a {@link User} object if logged in.
	 * If not logged in, generates a {@link Guest.User} object which lacks all permissions and roles.
	 * @param ctx request context.
	 * @return {@link User} object if the user is logged in, otherwise a {@link Guest.User} object.
	 */
	public static User getUserOrCreateGuestUser(Context ctx) {
		if (isLoggedIn(ctx)) {
			return getUser(ctx);
		} else {
			return new Guest.User();
		}
	}

	public static Optional<String> getUsername(Context ctx) {
		return getAuthImpl(ctx).getUsername(ctx);
	}

	public static Set<Roles> getUserRoles(Context ctx) {
		return getAuthImpl(ctx).getUserRoles(ctx);
	}

	private static Auth getAuthImpl(Context ctx) {
		if (ctx.headerMap().containsKey("Token") && !ctx.header("Token").isBlank()) {
			return tokenAuth;
		}

		return basicAuth;
	}
}
