package fi.ylihallila.server.authentication;

import io.javalin.core.security.Role;

public enum Roles implements Role {

	ANYONE,
	STUDENT,
	TEACHER,
	ADMIN

	// todo: inheritance: admin > teacher > student > anyone

}
