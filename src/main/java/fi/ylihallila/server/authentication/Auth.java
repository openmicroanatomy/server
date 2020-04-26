package fi.ylihallila.server.authentication;

import io.javalin.core.security.BasicAuthCredentials;
import io.javalin.core.security.Role;
import io.javalin.core.util.Header;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import kotlin.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Auth {

	private static Map<Pair<String, String>, List<Role>> userRoleMap = Map.of(
		new Pair<>("Aaron", "salasana"), List.of(Roles.STUDENT, Roles.TEACHER, Roles.ADMIN),
		new Pair<>("", ""),	 			 List.of(Roles.ANYONE)
	);

	public static void accessManager(Handler handler, Context ctx, Set<Role> permittedRoles) {
		try {
			if (permittedRoles.contains(Roles.ANYONE) || hasPermissions(ctx, permittedRoles)) {
				handler.handle(ctx);
			} else {
				ctx.header(Header.WWW_AUTHENTICATE, "Basic");
				ctx.status(401).json("Unauthorized");
			}
		} catch (Exception e) {
			e.printStackTrace();
			ctx.header(Header.WWW_AUTHENTICATE, "Basic");
			ctx.status(500).json("Internal server error");
		}
	}

	public static boolean hasPermissions(Context ctx, Set<Role> permittedRoles) {
		List<Role> roles = getUserRoles(ctx);
		AtomicBoolean hasPermission = new AtomicBoolean(false);

		permittedRoles.forEach(role -> {
			if (roles.contains(role)) {
				hasPermission.set(true);
			}
		});

		return hasPermission.get();
	}

	public static List<Role> getUserRoles(Context ctx) {
		try {
			BasicAuthCredentials auth = ctx.basicAuthCredentials();
			Pair<String, String> pair = new Pair<>(auth.getUsername(), auth.getPassword());

			return userRoleMap.getOrDefault(pair, Collections.emptyList());
		} catch (IllegalArgumentException e) {
			return Collections.emptyList();
		}
	}
}
