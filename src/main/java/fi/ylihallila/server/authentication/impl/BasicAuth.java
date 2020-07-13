package fi.ylihallila.server.authentication.impl;

import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.models.User;
import io.javalin.core.security.BasicAuthCredentials;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import kotlin.Pair;

import java.util.*;

public class BasicAuth implements Auth {

    private Map<Pair<String, String>, User> userMap = Map.of(
        new Pair<>("Aaron", "salasana"), new User(
            "fe034978-02d2-4612-a82d-d908b70bd1eb",
            "Aaron",
            "9f9ce49a-5101-4aa3-8c75-0d5935ad6525",
            Set.of(Roles.ADMIN, Roles.MANAGE_USERS, Roles.MANAGE_SLIDES, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS)
        ),
        new Pair<>("Demo", "Demo"), new User(
            "5cbf3d06-071a-4065-a24a-954a1109584b",
            "Demo",
            "9f9ce49a-5101-4aa3-8c75-0d5935ad6525",
            Set.of(Roles.MANAGE_SLIDES, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS)
        )
    );

    @Override
    public boolean isLoggedIn(Context ctx) {
        try {
            getUserObject(ctx);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public User getUser(Context ctx) {
        return getUserObject(ctx);
    }

    @Override
    public Optional<String> getUsername(Context ctx) {
        try {
            BasicAuthCredentials auth = ctx.basicAuthCredentials();

            return Optional.of(auth.getUsername());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean hasPermissions(Context ctx, Set<Role> permittedRoles) {
        Set<Roles> userRoles = getUserRoles(ctx);

        return permittedRoles.stream().anyMatch(userRoles::contains);
    }

    @Override
    public Set<Roles> getUserRoles(Context ctx) {
        try {
            BasicAuthCredentials auth = ctx.basicAuthCredentials();
            Pair<String, String> pair = new Pair<>(auth.getUsername(), auth.getPassword());

            return userMap.get(pair).getRoles();
        } catch (IllegalArgumentException e) {
            return Collections.emptySet();
        }
    }

    public User getUserObject(Context ctx) {
        try {
            BasicAuthCredentials auth = ctx.basicAuthCredentials();
            Pair<String, String> pair = new Pair<>(auth.getUsername(), auth.getPassword());

            if (userMap.containsKey(pair)) {
                return userMap.get(pair);
            } else {
                throw new UnauthorizedResponse();
            }
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedResponse();
        }
    }
}
