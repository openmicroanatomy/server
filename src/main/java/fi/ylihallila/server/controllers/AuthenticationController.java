package fi.ylihallila.server.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.authentication.impl.TokenAuth;
import fi.ylihallila.server.models.Organization;
import fi.ylihallila.server.models.User;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.openapi.annotations.*;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static fi.ylihallila.server.util.Config.Config;

public class AuthenticationController extends Controller {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @OpenApi(
        summary = "Check if user is authorized to edit given resource. The resource can represent either a workspace, subject or a project.",
        pathParams = @OpenApiParam(name = "id", required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class))
        },
        tags = { "authentication" },
        method = HttpMethod.GET,
        path = "/api/v0/auth/write/:id"
    )
    public void hasWritePermission(Context ctx) {
        String id = ctx.pathParam("id", String.class).get();

        ctx.status(200).json(hasWritePermission(ctx, id));
    }

    @OpenApi(
        summary = "Check if user is authorized to read given resource. The resource can represent either a workspace, subject or a project.",
        pathParams = @OpenApiParam(name = "id", required = true),
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Boolean.class))
        },
        tags = { "authentication" },
        method = HttpMethod.GET,
        path = "/api/v0/auth/read/:id"
    )
    public void hasReadPermission(Context ctx) {
        String id = ctx.pathParam("id", String.class).get();

        ctx.status(200).json(hasReadPermission(ctx, id));
    }

    /**
     * This method is used when authenticating with Basic Authentication. The endpoint returns an User object.
     * If the credentials are invalid, an NotAuthorizedException is thrown.
     */
    @OpenApi(
        summary = "Authenticate via Basic Authentication",
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
            @OpenApiResponse(status = "401"),
        },
        headers = {
            @OpenApiParam(name = "Authorization", description = "Use the Basic HTTP Authentication Scheme", required = true)
        },
        tags = { "authentication" },
        method = HttpMethod.GET,
        path = "/api/v0/auth/login"
    )
    public void login(Context ctx) {
        User user = Authenticator.getUser(ctx);

        ctx.status(200).json(user);
    }

    /**
     * Check that the given JWT was issued by Azure AD and is valid.
     * Creates a user and organization object if these do not exist.
     */
    @OpenApi(
        summary = "Authenticate via JWT",
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
            @OpenApiResponse(status = "401")
        },
        headers = {
            @OpenApiParam(name = "token", required = true)
        },
        tags = { "authentication" },
        method = HttpMethod.GET,
        path = "/api/v0/auth/verify"
    )
    public void verify(Context ctx) {
        var microsoftAuthenticationEnabled = Config.getBoolean("auth.microsoft.enabled");
        var allowUnknownOrganizations = Config.getBoolean("auth.microsoft.allow.any");
        var allowedOrganizations = Config.getStringList("auth.microsoft.allowed.organizations");

        if (!microsoftAuthenticationEnabled) {
            logger.debug("Tried to authenticate with Microsoft while it's disabled.");
            throw new UnauthorizedResponse("Microsoft Authentication is not enabled.");
        }

        DecodedJWT jwt = TokenAuth.validate(ctx);
        Session session = ctx.use(Session.class);

        String userId = jwt.getClaim("oid").asString();
        String organizationId = jwt.getClaim("tid").asString();

        Organization organization = session.find(Organization.class, organizationId);
        if (organization == null && !allowUnknownOrganizations && !allowedOrganizations.contains(organizationId)) {
            logger.debug("Tried to login using Microsoft with an illegal organization.");
            throw new UnauthorizedResponse("Provided organization is not allowed to log-in.");
        }

        User user = session.find(User.class, userId);
        if (user == null) {
            user = new User();
            user.setId(userId);
            user.setName(jwt.getClaim("name").asString());
            user.setEmail(jwt.getClaim("email").asString());
            user.setOrganization(organizationId);
            user.setRoles(Set.of());

           // TODO: Reimplement permissions or config for personal projects

            session.save(user);
        }

        // TODO: Update name & email if JWT has new details.
        ctx.status(200).json(user);
    }
}
