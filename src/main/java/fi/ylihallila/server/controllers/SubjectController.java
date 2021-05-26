package fi.ylihallila.server.controllers;

import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.exceptions.UnprocessableEntityResponse;
import fi.ylihallila.server.models.Subject;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.models.Workspace;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiFormParam;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.apache.commons.lang3.NotImplementedException;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectController extends Controller implements CrudHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @OpenApi(
        summary = "Create a new subject",
        tags = { "subject" },
        formParams = {
            @OpenApiFormParam(name = "workspace-id", required = true),
            @OpenApiFormParam(name = "subject-name", required = true)
        },
        responses = {
            @OpenApiResponse(status = "201", content = @OpenApiContent(from = Subject.class)),
            @OpenApiResponse(status = "403"),
            @OpenApiResponse(status = "404")
        }
    )
    @Override public void create(@NotNull Context ctx) {
        Allow(ctx, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS);

        String workspaceId = ctx.formParam("workspace-id", String.class).get();
        String subjectName = ctx.formParam("subject-name", String.class).get();
        Session session = ctx.use(Session.class);
        User       user = Authenticator.getUser(ctx);

        Workspace workspace = session.find(Workspace.class, workspaceId);

        if (workspace == null) {
            throw new NotFoundResponse();
        }

        if (!workspace.hasPermission(user)) {
            throw new ForbiddenResponse();
        }

        Subject subject = new Subject(subjectName, workspace);
        workspace.addSubject(subject);

        ctx.status(201).json(subject);

        logger.info("Subject {} [Workspace: {}] created by {}", subjectName, workspace.getName(), Authenticator.getUsername(ctx).orElse("Unknown"));
    }

    @OpenApi(
        summary = "Delete given subject",
        tags = { "subject" },
        formParams = {
            @OpenApiFormParam(name = "name", required = true)
        },
        responses = {
            @OpenApiResponse(status = "200"),
            @OpenApiResponse(status = "403"),
        }
    )
    @Override public void delete(@NotNull Context ctx, @NotNull String id) {
        Allow(ctx, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS);

        Session session = ctx.use(Session.class);
        User user = Authenticator.getUser(ctx);

        Subject subject = getSubject(id, session, user);

        session.delete(subject);

        logger.info("Subject {} deleted by {}", id, user.getName());
    }

    @OpenApi(
        summary = "Update given subject",
        tags = { "subject" },
        formParams = {
            @OpenApiFormParam(name = "name", required = true)
        },
        responses = {
            @OpenApiResponse(status = "200", content = @OpenApiContent(from = Subject.class)),
            @OpenApiResponse(status = "403"),
            @OpenApiResponse(status = "422"),
        }
    )
    @Override public void update(@NotNull Context ctx, @NotNull String id) {
        Allow(ctx, Roles.MANAGE_PERSONAL_PROJECTS, Roles.MANAGE_PROJECTS);

        Session session = ctx.use(Session.class);
        User user = Authenticator.getUser(ctx);

        Subject subject = getSubject(id, session, user);

        if (subject.getName().equals("Copied Projects")) {
            throw new UnprocessableEntityResponse("Not allowed to rename copied projects.");
        }

        subject.setName(ctx.formParam("subject-name", subject.getName()));

        ctx.status(200).json(subject);

        logger.info("Subject {} edited by {}", id, user.getName());
    }

    @OpenApi(ignore = true)
    @Override public void getAll(@NotNull Context context) {
        throw new NotFoundResponse();
    }

    @OpenApi(ignore = true)
    @Override public void getOne(@NotNull Context ctx, @NotNull String id) {
        throw new NotFoundResponse();
    }

    /**
     * Gets a subject and checks that the given user has write permissions for that subject.
     *
     * @param id subject id
     * @param session database session
     * @param user user who is accessing the data
     * @return Subject
     * @throws NotFoundResponse when unknown subject id
     * @throws ForbiddenResponse when user has no permission for provided session
     */
    private Subject getSubject(String id, Session session, User user) {
        Subject subject = session.find(Subject.class, id);

        if (subject == null) {
            throw new NotFoundResponse();
        }

        if (!subject.getWorkspace().hasPermission(user)) {
            throw new ForbiddenResponse();
        }

        return subject;
    }
}
