package fi.ylihallila.server.authentication.impl;

import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.Database;
import fi.ylihallila.server.Util;
import fi.ylihallila.server.authentication.Auth;
import fi.ylihallila.server.models.User;
import io.javalin.core.security.BasicAuthCredentials;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import kotlin.Pair;
import org.hibernate.Session;

import java.util.*;

public class BasicAuth implements Auth {

    private final Map<Pair<String, String>, String> userMap = Map.of(
        new Pair<>("Aaron", "salasana"), "dfa4d992-067e-40bc-b31e-58f8a8b77044",
        new Pair<>("Demo", "demo"),      "2c8ca23a-17f1-48f7-804b-8defd8808fb6"
    );

    static {
        Session session = Database.getSession();
        session.beginTransaction();

        var user = new User(
            "dfa4d992-067e-40bc-b31e-58f8a8b77044",
            "Aaron",
            Set.of(Roles.ADMIN, Roles.MANAGE_USERS, Roles.MANAGE_SLIDES, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS),
            Util.getOrganization("9f9ce49a-5101-4aa3-8c75-0d5935ad6525")
        );

        session.saveOrUpdate(user);

        user = new User(
            "2c8ca23a-17f1-48f7-804b-8defd8808fb6",
            "Demo",
            Set.of(Roles.MANAGE_SLIDES, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS),
            Util.getOrganization("9f9ce49a-5101-4aa3-8c75-0d5935ad6525")
        );

        session.saveOrUpdate(user);

        session.getTransaction().commit();
        session.close();
    }

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
        return getUserObject(ctx).getRoles();
//        try {
//            BasicAuthCredentials auth = ctx.basicAuthCredentials();
//            Pair<String, String> pair = new Pair<>(auth.getUsername(), auth.getPassword());
//
//            return userMap.get(pair).getRoles();
//        } catch (IllegalArgumentException e) {
//            return Collections.emptySet();
//        }
    }

    public User getUserObject(Context ctx) {
        try {
            BasicAuthCredentials auth = ctx.basicAuthCredentials();
            Pair<String, String> pair = new Pair<>(auth.getUsername(), auth.getPassword());

            if (userMap.containsKey(pair)) {
                Session session = Database.getSession();
                session.beginTransaction();

                User user = session.find(User.class, userMap.get(pair));

                session.getTransaction();
                session.close();

                return user;
            } else {
                throw new UnauthorizedResponse();
            }
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedResponse();
        }
    }
}
