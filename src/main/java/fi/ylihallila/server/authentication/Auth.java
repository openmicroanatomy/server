package fi.ylihallila.server.authentication;

import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.models.User;
import io.javalin.core.security.Role;
import io.javalin.http.Context;

import java.util.Optional;
import java.util.Set;

public interface Auth {

    boolean isLoggedIn(Context ctx);

    User getUser(Context ctx);

    Optional<String> getUsername(Context ctx);

    boolean hasPermissions(Context ctx, Set<Role> permittedRoles);

    Set<Roles> getUserRoles(Context ctx);

}
