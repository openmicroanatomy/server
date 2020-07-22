package fi.ylihallila.server.authentication.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.util.Database;
import fi.ylihallila.server.authentication.Auth;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.Constants;
import io.javalin.core.security.BasicAuthCredentials;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import kotlin.Pair;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BasicAuth implements Auth {

    private static final Logger logger = LoggerFactory.getLogger(BasicAuth.class);
    public static Map<Pair<String, String>, String> userMap = new HashMap<>();

    static {
        try {
            Session session = Database.getSession();
            session.beginTransaction();

            JsonArray users = (JsonArray) JsonParser.parseString(Files.readString(Path.of(Constants.USERS_FILE)));

            for (JsonElement element : users) {
                JsonObject data = element.getAsJsonObject();
                JsonArray array = data.getAsJsonArray("roles");

                Set<Roles> roles = IntStream.range(0, array.size())
                        .mapToObj(array::get)
                        .map(o -> Roles.valueOf(o.getAsString()))
                        .collect(Collectors.toSet());

                User user = new User();
                user.setId(data.get("id").getAsString());
                user.setName(data.get("name").getAsString());
                user.setEmail(data.get("email").getAsString());
                user.setOrganization(data.get("organization").getAsString());
                user.setRoles(roles);

                userMap.put(new Pair<>(data.get("username").getAsString(), data.get("password").getAsString()), data.get("id").getAsString());

                session.saveOrUpdate(user);
            }

            session.getTransaction().commit();
            session.close();
        } catch (IOException e) {
            logger.error("Error while reading BasicAuth user.json", e);
        }
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
    public boolean hasRoles(Context ctx, Set<Role> permittedRoles) {
        Set<Roles> userRoles = getUserRoles(ctx);

        return permittedRoles.stream().anyMatch(userRoles::contains);
    }

    @Override
    public Set<Roles> getUserRoles(Context ctx) {
        return getUserObject(ctx).getRoles();
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
