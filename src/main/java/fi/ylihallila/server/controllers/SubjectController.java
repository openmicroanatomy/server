package fi.ylihallila.server.controllers;

import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.models.Subject;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.models.Workspace;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void createSubject(Context ctx) {
        String workspaceId = ctx.formParam("workspace-id", String.class).get();
        String subjectName = ctx.formParam("subject-name", String.class).get();
        Session session = ctx.use(Session.class);
        User user = Authenticator.getUser(ctx);

        Workspace workspace = session.find(Workspace.class, workspaceId);

        if (workspace == null) {
            throw new NotFoundResponse();
        }

        if (!workspace.hasPermission(user)) {
            throw new ForbiddenResponse();
        }

        Subject subject = new Subject(subjectName, workspace);

        session.save(subject);

        ctx.status(200).json(subject);

        logger.info("Subject {} [Workspace: {}] created by {}", subjectName, workspace.getName(), Authenticator.getUsername(ctx).orElse("Unknown"));
    }

    public void deleteSubject(Context ctx) {
        String id = ctx.pathParam("subject-id", String.class).get();
        Session session = ctx.use(Session.class);
        User user = Authenticator.getUser(ctx);

        Subject subject = getSubject(id, session, user);

        session.delete(subject);

        logger.info("Subject {} deleted by {}", id, user.getName());
    }

    public void updateSubject(Context ctx) {
        String id = ctx.pathParam("subject-id", String.class).get();
        Session session = ctx.use(Session.class);
        User user = Authenticator.getUser(ctx);

        Subject subject = getSubject(id, session, user);
        subject.setName(ctx.formParam("subject-name", subject.getName()));

        logger.info("Subject {} edited by {}", id, user.getName());
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
