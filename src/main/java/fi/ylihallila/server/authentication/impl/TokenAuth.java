package fi.ylihallila.server.authentication.impl;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.util.Database;
import fi.ylihallila.server.authentication.Auth;
import fi.ylihallila.server.models.User;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;
import java.util.Set;

import static fi.ylihallila.server.util.Config.Config;

public class TokenAuth implements Auth {

    private static final Logger logger = LoggerFactory.getLogger(TokenAuth.class);

    /**
     * GUID of Microsoft Application, used to verify that the
     * provided JWT was supplied by our application.
     */
    private static final String APP_ID = Config.getString("microsoft.app.id");

    /**
     * Each JWT can be signed using a different private key. JWKs provide
     * a list of these, including the public key to verify the signature.

     * @see <a href="https://tools.ietf.org/html/rfc7517">JWK specification</a>
     */
    private static JwkProvider provider;

    public TokenAuth() {
        try {
            provider = new UrlJwkProvider(new URL(Config.getString("microsoft.jwk.provider")));
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public boolean isLoggedIn(Context ctx) {
        try {
            validate(ctx);

            return true;
        } catch (UnauthorizedResponse e) {
            return false;
        }
    }

    @Override
    public User getUser(Context ctx) {
        DecodedJWT jwt = validate(ctx);

        return getUser(jwt);
    }

    @Override
    public Optional<String> getUsername(Context ctx) {
        DecodedJWT jwt = validate(ctx);

        return Optional.of(getUser(jwt).getName());
    }

    @Override
    public boolean hasRoles(Context ctx, Set<Role> permittedRoles) {
        DecodedJWT jwt = validate(ctx);
        Set<Roles> userRoles = getUser(jwt).getRoles();

        return userRoles.contains(Roles.ADMIN) || permittedRoles.stream().anyMatch(userRoles::contains);
    }

    @Override
    public Set<Roles> getUserRoles(Context ctx) {
        DecodedJWT jwt = validate(ctx);

        return getUser(jwt).getRoles();
    }

    /* Private API */

    private User getUser(DecodedJWT jwt) {
        Session session = Database.openSession();
        session.beginTransaction();

        User user = session.find(User.class, jwt.getClaim("oid").asString());

        session.getTransaction().commit();
        session.close();

        if (user == null) {
            throw new NotFoundResponse("No user found with given OID");
        }

        return user;
    }

    public static DecodedJWT validate(Context ctx) {
        String token = ctx.header("token", String.class).get();

        try {
            DecodedJWT jwt = JWT.decode(token);
            Jwk jwk = provider.get(jwt.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(),null);

            JWTVerifier verifier = JWT.require(algorithm)
                .acceptExpiresAt(5 * 3600)
                .withAudience(APP_ID)
                .build();

            return verifier.verify(jwt);
        } catch (Exception e) {
            logger.debug("Error while authenticating", e);
            throw new UnauthorizedResponse(e.getLocalizedMessage());
        }
    }
}
