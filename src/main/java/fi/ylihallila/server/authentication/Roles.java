package fi.ylihallila.server.authentication;

import io.javalin.core.security.Role;

public enum Roles implements Role {

	ANYONE,
	ADMIN,

	MANAGE_SLIDES,

	MANAGE_PERSONAL_PROJECTS,
	MANAGE_PROJECTS,

	// todo: inheritance: admin > teacher > student > anyone

}
