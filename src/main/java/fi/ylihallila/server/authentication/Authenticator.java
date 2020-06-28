package fi.ylihallila.server.authentication;

import fi.ylihallila.server.authentication.impl.Auth;
import fi.ylihallila.server.authentication.impl.BasicAuth;
import fi.ylihallila.server.authentication.impl.TokenAuth;
import fi.ylihallila.server.gson.Error;
import fi.ylihallila.server.gson.User;
import io.javalin.core.security.Role;
import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Authenticator {

	private static final Logger logger = LoggerFactory.getLogger(Authenticator.class);

	private static BasicAuth basicAuth = new BasicAuth();
	private static TokenAuth tokenAuth = new TokenAuth();

	public static void accessManager(Handler handler, Context ctx, Set<Role> permittedRoles) {
		try {
			if (permittedRoles.contains(Roles.ANYONE) || hasPermissions(ctx, permittedRoles)) {
				handler.handle(ctx);
			} else {
				ctx.status(401).json(new Error("Unauthorized. Use Basic authentication or provide `Token` header."));
			}
		} catch (NotFoundResponse e) {
			ctx.status(404).json(new Error(e.getMessage()));
		} catch (UnauthorizedResponse e) {
			ctx.status(401).json(new Error(e.getLocalizedMessage()));
		} catch (Exception e) {
			ctx.status(500).json(new Error("Internal server error"));
			logger.error("Error while authenticating", e);
		}
	}

	public static boolean isLoggedIn(Context ctx) {
		return getAuthImpl(ctx).isLoggedIn(ctx);
	}

	public static boolean hasPermissions(Context ctx, Role permittedRole) {
		return hasPermissions(ctx, Set.of(permittedRole));
	}

	public static boolean hasPermissions(Context ctx, Set<Role> permittedRoles) {
		return getAuthImpl(ctx).hasPermissions(ctx, permittedRoles);
	}

	public static User getUser(Context ctx) {
		return getAuthImpl(ctx).getUser(ctx);
	}

	public static Optional<String> getUsername(Context ctx) {
		return getAuthImpl(ctx).getUsername(ctx);
	}

	public static List<Roles> getUserRoles(Context ctx) {
		return getAuthImpl(ctx).getUserRoles(ctx);
	}

	private static Auth getAuthImpl(Context ctx) {
		if (ctx.headerMap().containsKey("Token") && !ctx.header("Token").isBlank()) {
			return tokenAuth;
		}

		return basicAuth;
	}
}
