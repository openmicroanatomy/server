package fi.ylihallila.server.controllers;

import fi.ylihallila.server.authentication.Auth;
import io.javalin.http.Context;

import java.util.Map;
import java.util.UUID;

// todo: wip
public class UserController {

	private String username;

	private Map<String, String> users = Map.of(
		"aaron", "aaron",
		"demo",  "demo"
	);

	public void getAllUsers(Context ctx) {
		ctx.json(users.keySet());
	}

	public void createUser(Context ctx) {
		users.put(ctx.formParam("username"), ctx.formParam("password"));
	}

	public void getUser(Context ctx) {
		ctx.json(users.get(ctx.pathParam("username")));
	}

	public void updateUser(Context ctx) {
		users.replace(ctx.formParam("username"), ctx.formParam("password"));
	}

	public void deleteUser(Context ctx) {
		users.remove(ctx.pathParam("username"));
	}

	public void login(Context ctx) {
		ctx.json(Auth.getUserRoles(ctx));
	}

	private String randomId() {
		return UUID.randomUUID().toString();
	}
}
