package fi.ylihallila.server.controllers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.models.Error;
import fi.ylihallila.server.models.Organization;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.Database;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.openapi.annotations.*;
import io.javalin.plugin.rendering.vue.JavalinVue;
import io.javalin.plugin.rendering.vue.VueComponent;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class DashboardController extends Controller {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void index(Context ctx) {
        if (Constants.IS_SETUP) {
            dashboard(ctx);
        } else {
            setup(ctx);
        }
    }

    @OpenApi(
        tags = "server",
        summary = "Initialize server for first-time use.",
        method = HttpMethod.POST,
        path = "/initialize",
        formParams = {
            @OpenApiFormParam(name = "organizationName", required = true),
            @OpenApiFormParam(name = "email", required = true),
            @OpenApiFormParam(name = "name", required = true),
            @OpenApiFormParam(name = "password", required = true),
            @OpenApiFormParam(name = "repeatPassword", required = true),
        },
        responses = {
            @OpenApiResponse(status = "200", description = "Server successfully setup"),
            @OpenApiResponse(status = "400", description = "Invalid input")
        }
    )
    public void initialize(Context ctx) {
        if (Constants.IS_SETUP) throw new UnauthorizedResponse("Server already setup.");

        String organizationName = ctx.formParam("organizationName", String.class).get();
        String email            = ctx.formParam("email", String.class).get();
        String name             = ctx.formParam("name", String.class).check(s -> s.length() > 3, "Name must be at least 4 characters.").get();
        String password         = ctx.formParam("password", String.class).check(s -> s.length() >= 5, "Password must be at least 5 characters.").get();
        String repeatPassword   = ctx.formParam("repeatPassword", String.class).check(s -> s.length() >= 5, "Password must be at least 5 characters.").get();

        if (!(password.equals(repeatPassword))) {
            throw new BadRequestResponse("Passwords do not match.");
        }

        Session session = Database.openSession();

        try  {
            session.beginTransaction();

            Organization organization = new Organization();
            organization.setName(organizationName);
            organization.setId(UUID.randomUUID());

            session.save(organization);

            User user = new User();
            user.setId(UUID.randomUUID());
            user.setOrganization(organization);
            user.setEmail(email);
            user.setName(name);
            user.hashPassword(password);
            user.setRoles(Set.of(Roles.ADMIN));

            session.save(user);
            session.getTransaction().commit();

            ctx.status(200);

            Constants.IS_SETUP = true;

            logger.info("Server initialized: Organization {} created by {} [Email: {}]", organizationName, user, email);
        } catch (Exception e) {
            logger.error("Error while initialization", e);

            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }

            ctx.status(400).json(new Error(e.getMessage()));
        } finally {
            session.close();
        }
    }

    private void setup(Context ctx) {
        new VueComponent("setup").handle(ctx);
    }

    private void dashboard(Context ctx) {
        JavalinVue.stateFunction = c -> new ObjectMapper().convertValue(getServerState(), Map.class);
        new VueComponent("dashboard").handle(ctx);
    }

    /* Private API */

    /**
     * Collects information about the server state from different sources into one location.
     * @return a {@link ServerState} object
     */
    private ServerState getServerState() {
        ServerState state = new ServerState();

        Path root = new File(System.getProperty("user.dir", ".")).toPath().getRoot();

        if (root != null) {
            File diskPartition = root.toFile();

            state.DiskSpaceFree = diskPartition.getFreeSpace();
            state.DiskTotalSpace = diskPartition.getTotalSpace();
        }

        state.RAMTotal = Runtime.getRuntime().maxMemory();
        state.RAMUsage = Runtime.getRuntime().totalMemory();

        state.CPUs = Runtime.getRuntime().availableProcessors();

        Session session = Database.openSession();
        session.beginTransaction();

        state.Users         = session.createQuery("SELECT COUNT (*) FROM User", Long.class).getSingleResult();
        state.Organizations = session.createQuery("SELECT COUNT (*) FROM Organization", Long.class).getSingleResult();
        state.Workspaces    = session.createQuery("SELECT COUNT (*) FROM Workspace", Long.class).getSingleResult();
        state.Courses       = session.createQuery("SELECT COUNT (*) FROM Subject", Long.class).getSingleResult();
        state.Lessons       = session.createQuery("SELECT COUNT (*) FROM Project", Long.class).getSingleResult();
        state.Slides        = session.createQuery("SELECT COUNT (*) FROM Slide", Long.class).getSingleResult();

        session.getTransaction().commit();
        session.close();

        return state;
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
    private static class ServerState {

        @JsonProperty
        public long DiskSpaceFree;

        @JsonProperty
        public long DiskTotalSpace;

        @JsonProperty
        public long RAMUsage;

        @JsonProperty
        public long RAMTotal;

        @JsonProperty
        public int CPUs;

        @JsonProperty
        public long Users;

        @JsonProperty
        public long Organizations;

        @JsonProperty
        public long Workspaces;

        @JsonProperty
        public long Courses;

        @JsonProperty
        public long Lessons;

        @JsonProperty
        public long Slides;

    }
}
