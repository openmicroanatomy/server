package fi.ylihallila.server.authentication.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.ylihallila.server.authentication.Auth;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.Database;
import fi.ylihallila.server.util.PasswordHelper;
import io.javalin.core.security.BasicAuthCredentials;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BasicAuth implements Auth {

    private static final Logger logger = LoggerFactory.getLogger(BasicAuth.class);

    static {
        try {
            Session session = Database.openSession();
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
                user.setPassword(data.get("password").getAsString());
                user.setOAuth(false);
                user.setRoles(EnumSet.copyOf(roles));

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
//        if (!ctx.basicAuthCredentialsExist()) {
//            return false;
//        }

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
        if (!ctx.basicAuthCredentialsExist()) {
            return false;
        }

        Set<Roles> userRoles = getUserRoles(ctx);

        return permittedRoles.stream().anyMatch(userRoles::contains);
    }

    @Override
    public Set<Roles> getUserRoles(Context ctx) {
        return getUserObject(ctx).getRoles();
    }

    public User getUserObject(Context ctx) {
        try {
            Session session = ctx.use(Session.class);

//            if (!ctx.basicAuthCredentialsExist()) {
//                throw new UnauthorizedResponse("No username or password provided.");
//            }

            BasicAuthCredentials auth = ctx.basicAuthCredentials();

            User user = session.createQuery("from User where email = :email", User.class)
                    .setParameter("email", auth.getUsername()).getSingleResult();

            if (user == null) {
                throw new UnauthorizedResponse("Invalid email or password");
            }

            if (PasswordHelper.validatePassword(auth.getPassword(), user.getPassword())) {
                return user;
            } else {
                throw new UnauthorizedResponse("Invalid email or password");
            }
        } catch (NoResultException | NonUniqueResultException e) {
            throw new UnauthorizedResponse("Invalid email or password");
        } catch (UnauthorizedResponse e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error while authenticating", e);
            throw new UnauthorizedResponse("Error when logging in. This incident has been logged.");
        }
    }
}
