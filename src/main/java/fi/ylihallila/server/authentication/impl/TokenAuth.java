package fi.ylihallila.server.authentication.impl;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import fi.ylihallila.remote.commons.Roles;
import fi.ylihallila.server.gson.User;
import fi.ylihallila.server.repositories.Repos;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TokenAuth implements Auth {

    private static JwkProvider provider;
    private static final Logger logger = LoggerFactory.getLogger(TokenAuth.class);

    public TokenAuth() {
        try {
            provider = new UrlJwkProvider(new URL("https://login.microsoftonline.com/common/discovery/keys"));
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
    public boolean hasPermissions(Context ctx, Set<Role> permittedRoles) {
        DecodedJWT jwt = validate(ctx);

        return permittedRoles.stream().anyMatch(getUser(jwt).getRoles()::contains);
    }

    @Override
    public Set<Roles> getUserRoles(Context ctx) {
        DecodedJWT jwt = validate(ctx);

        return getUser(jwt).getRoles();
    }

    /* Private API */

    private User getUser(DecodedJWT jwt) {
        Optional<User> user = Repos.getUserRepo().getById(jwt.getClaim("oid").asString());

        if (user.isPresent()) {
            return user.get();
        }

        throw new NotFoundResponse("No user found with given OID");
    }

    public static DecodedJWT validate(Context ctx) {
        String token = ctx.header("token", String.class).get();

        try {
            DecodedJWT jwt = JWT.decode(token);
            Jwk jwk = provider.get(jwt.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(),null);

            JWTVerifier verifier = JWT.require(algorithm)
                .withAudience("eccc9211-faa5-40d5-9ff9-7a5087dbcadb")
                .build();

            return verifier.verify(jwt);
        } catch (Exception e) {
            logger.debug("Error while authenticating", e);
            throw new UnauthorizedResponse(e.getLocalizedMessage());
        }
    }
}
