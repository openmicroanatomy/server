package fi.ylihallila.server.controllers;

import fi.ylihallila.server.models.Error;
import fi.ylihallila.server.models.PasswordResetRequest;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.Util;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.openapi.annotations.*;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;

import static fi.ylihallila.server.util.Config.Config;

public class PasswordController extends Controller {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @OpenApi(
        summary = "Reset password",
        responses = {
            @OpenApiResponse(status = "200"),
            @OpenApiResponse(status = "404", description = "Token has expired or doesn't exist")
        },
        pathParams = {
            @OpenApiParam(name = "token", required = true)
        },
        formParams = {
            @OpenApiFormParam(name = "password"),
        },
        tags = { "password" },
        method = HttpMethod.POST,
        path = "/api/v0/password/set/:token"
    )
    public void reset(Context ctx) {
        Session session = ctx.use(Session.class);
        String password = ctx.formParam("password", String.class).get();
        String token    = ctx.pathParam("token", String.class).get();

        try {
            PasswordResetRequest pwResetRequest = session.createQuery("from PasswordResetRequest where token = :token", PasswordResetRequest.class)
                    .setParameter("token", token).getSingleResult();

            if (pwResetRequest.hasExpired()) {
                session.delete(pwResetRequest);

                throw new NotFoundResponse("Token has expired");
            }

            User user = pwResetRequest.getUser();
            user.hashPassword(password);

            session.delete(pwResetRequest);
            session.update(user);

            ctx.status(200);

            logger.info("User {} ({}) password reset by {}", user.getEmail(), user.getId(), ctx.req.getRemoteAddr());
        } catch (NoResultException e) {
            throw new NotFoundResponse("Token not found");
        } catch (Exception e) {
            logger.error("Error while resetting password", e);

            throw new InternalServerErrorResponse("Error while resetting password");
        }
    }

    @OpenApi(
        summary = "Used to send a password recovery email (including link and recovery token) to the user.",
        responses = {
            @OpenApiResponse(status = "200"),
            @OpenApiResponse(status = "404", description = "User not found"),
        },
        formParams = {
            @OpenApiFormParam(name = "email", required = true)
        },
        tags = { "password" },
        method = HttpMethod.POST,
        path = "/api/v0/password/recovery"
    )
    public void recovery(Context ctx) {
        String email = ctx.formParam("email", String.class).get();
        Session session = ctx.use(Session.class);

        try {
            User user = session.createQuery("from User where email = :email", User.class)
                    .setParameter("email", email).getSingleResult();

            PasswordResetRequest passwordResetRequest = new PasswordResetRequest(user);
            session.save(passwordResetRequest);

            String token = passwordResetRequest.getToken();
            String port = Config.getString("server.port.insecure");  // TODO: Secure server
            String host = Config.getString("server.host");

            String body = Util.getResourceFileAsString("email/password_recovery.html")
                    .replace("{{token}}", token)
                    .replace("{{link}}", String.format(Constants.PASSWORD_RESET_URL, host, port, token));

//			Mailer mailer = new Mailer();
//			mailer.sendMail(user.getEmail(), "QuPath Edu Password Recovery", body);

            logger.info(body);

            ctx.status(200);
        } catch (NoResultException e) {
            ctx.status(404);
//		} catch (MessagingException e) {
//			ctx.status(500).json(new Error("Error while sending recovery token email."));
//			logger.error("Error while sending recovery token email", e);
        } catch (Exception e) {
            ctx.status(500).json(new Error("Internal Server Error while generating password recovery token."));
            logger.error("Error while generating recovery token for {}. {}", email, e);
        }
    }
}
