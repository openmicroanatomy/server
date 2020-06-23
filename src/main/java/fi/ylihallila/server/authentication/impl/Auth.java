package fi.ylihallila.server.authentication.impl;

import fi.ylihallila.server.authentication.Roles;
import fi.ylihallila.server.gson.User;
import io.javalin.core.security.Role;
import io.javalin.http.Context;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface Auth {

    boolean isLoggedIn(Context ctx);

    User getUser(Context ctx);

    Optional<String> getUsername(Context ctx);

    boolean hasPermissions(Context ctx, Set<Role> permittedRoles);

    List<Roles> getUserRoles(Context ctx);

}
