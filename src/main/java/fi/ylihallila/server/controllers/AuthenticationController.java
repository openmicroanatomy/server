package fi.ylihallila.server.controllers;

import com.auth0.jwt.interfaces.DecodedJWT;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.authentication.impl.TokenAuth;
import fi.ylihallila.server.models.User;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.*;
import org.hibernate.Session;

import java.util.Set;

public class AuthenticationController extends Controller {

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
     * This method checks that the given JWT was issued by Azure AD and is valid.
     *
     * On success, returns an User object.
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
        DecodedJWT jwt = TokenAuth.validate(ctx);
        String id = jwt.getClaim("oid").asString();

        Session session = ctx.use(Session.class);
        User user = session.find(User.class, id);

        if (user == null) {
            user = new User();
            user.setId(id);
            user.setName(jwt.getClaim("name").asString());
            user.setEmail(jwt.getClaim("email").asString());
            user.setOrganization(jwt.getClaim("tid").asString());
            user.setRoles(Set.of());

           // TODO: Reimplement permissions or config for personal projects

            session.save(user);
        }

        ctx.status(200).json(user);
    }
}
