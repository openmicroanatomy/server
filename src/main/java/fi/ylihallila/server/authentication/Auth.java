package fi.ylihallila.server.authentication;

import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.models.User;
import io.javalin.core.security.Role;
import io.javalin.http.Context;

import java.util.Optional;
import java.util.Set;

public interface Auth {

    /*

     TODO: Rework authentication.
          - move functionality to User class
          - rename isLoggedIn to something more accurate like "isValidCredentials"
          - getUsername() could just use the User object?
          - really only needs validate(Request request) and getUser(); ?
     */


    boolean isLoggedIn(Context ctx);

    User getUser(Context ctx);

    Optional<String> getUsername(Context ctx);

    boolean hasRoles(Context ctx, Set<Role> permittedRoles);

    Set<Roles> getUserRoles(Context ctx);

}
