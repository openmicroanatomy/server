package fi.ylihallila.server;

import com.google.inject.Injector;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.controllers.*;
import fi.ylihallila.server.util.Constants;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.rendering.vue.JavalinVue;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import io.javalin.plugin.rendering.vue.VueComponent;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.core.security.SecurityUtil.roles;
import static fi.ylihallila.remote.commons.Roles.*;
import static fi.ylihallila.server.util.Config.Config;

public class SecureServer {

    private Logger logger = LoggerFactory.getLogger(SecureServer.class);
    private Javalin app = Javalin.create(config -> {
        config.accessManager(Authenticator::accessManager);
        config.showJavalinBanner = false;
        config.enableWebjars();
        config.requestCacheSize = Long.MAX_VALUE;
        config.addStaticFiles("/static", Location.CLASSPATH);
        config.addStaticFiles("/logos", Path.of("organizations").toAbsolutePath().toString(), Location.EXTERNAL);

        JavalinVue.rootDirectory("/vue", Location.CLASSPATH);

        config.server(() -> {
            Server server = new Server();

            if (Constants.SECURE_SERVER) {
                config.enforceSsl = true;

                ServerConnector sslConnector = new ServerConnector(server, getSslContextFactory());
                sslConnector.setPort(Config.getInt("server.port.secure"));
                server.addConnector(sslConnector);
            } else {
                ServerConnector connector = new ServerConnector(server);
                connector.setPort(Config.getInt("server.port.insecure"));
                server.addConnector(connector);
            }

            return server;
        });
    }).start();

    private OrganizationController OrganizationController;
    private WorkspaceController WorkspaceController;
    private ProjectController ProjectController;
    private BackupController BackupController;
    private SlideController SlideController;
    private UserController UserController;

    public SecureServer() {
        createControllers();

        app.get("/", new VueComponent("index"),          roles(ADMIN));
        app.get("/upload", new VueComponent("uploader"), roles(ADMIN));

        app.routes(() -> path("/api/v0/", () -> {
            before(ctx -> { // TODO: Annotate methods with @Database; only these have Session available to save resources?
                logger.debug("Creating Database Session for Request");

                Session session = Database.getSession();
                session.beginTransaction();

                ctx.register(Session.class, session);
            });

            after(ctx -> {
                logger.debug("Destroying Database Session for Request");

                Session session = ctx.use(Session.class);

                session.getTransaction().commit();
                session.close();
            });

            /* Authentication */
            path("users", () -> {
                get(UserController::getAllUsers,                roles(MANAGE_USERS));
                get("login", UserController::login,             roles(ANYONE));
                get("verify", UserController::verify,           roles(ANYONE));
                get("write/:id", UserController::hasPermission, roles(ANYONE));

                path(":user-id", () -> {
                    get(UserController::getUser,    roles(MANAGE_USERS));
                    put(UserController::updateUser, roles(MANAGE_USERS));
                });
            });

            /* Upload */
            get("upload",  SlideController::upload, roles(MANAGE_SLIDES));
            post("upload", SlideController::upload, roles(MANAGE_SLIDES));

            /* Slides */
            path("slides", () -> {
                get(SlideController::getAllSlides, roles(ANYONE));

                path(":slide-id", () -> {
                    get(SlideController::getSlideProperties, roles(ANYONE));
                    put(SlideController::updateSlide,        roles(MANAGE_SLIDES));
                    delete(SlideController::deleteSlide,     roles(MANAGE_SLIDES));
                    get("tile/:tileX/:tileY/:level/:tileWidth/:tileHeight", SlideController::renderTile, roles(ANYONE));
                });
            });

            /* Workspaces */
            path("workspaces", () -> {
                get(WorkspaceController::getAllWorkspaces, roles(ANYONE));
                post(WorkspaceController::createWorkspace, roles(MANAGE_PROJECTS));

                path(":workspace-id", () -> {
                    get(WorkspaceController::getWorkspace,       roles(ANYONE));
                    put(WorkspaceController::updateWorkspace,    roles(MANAGE_PROJECTS));
                    delete(WorkspaceController::deleteWorkspace, roles(MANAGE_PROJECTS));
                });
            });

            /* Projects */
            path("projects", () -> {
                get(ProjectController::getAllProjects,                          roles(ADMIN));
                post(ProjectController::createProject,                          roles(MANAGE_PROJECTS));
                post("personal", ProjectController::createPersonalProject, roles(MANAGE_PERSONAL_PROJECTS));

                path(":project-id", () -> {
                    get(ProjectController::downloadProject,  roles(ANYONE));
                    put(ProjectController::updateProject,    roles(MANAGE_PERSONAL_PROJECTS, MANAGE_PROJECTS));
                    delete(ProjectController::deleteProject, roles(MANAGE_PERSONAL_PROJECTS, MANAGE_PROJECTS));
                    post(ProjectController::uploadProject,   roles(MANAGE_PERSONAL_PROJECTS, MANAGE_PROJECTS));
                });
            });

            /* Backups */
            path("backups", () -> {
                get(BackupController::getAllBackups, roles(MANAGE_PROJECTS));

                get("restore/:file/:timestamp", BackupController::restore, roles(MANAGE_PROJECTS));
            });

            /* Organizations */
            path("organizations", () -> {
                get(OrganizationController::getAllOrganizations, roles(ANYONE));
            });
        }));
    }

    private void createControllers() {
        this.OrganizationController = new OrganizationController();
        this.WorkspaceController = new WorkspaceController();
        this.ProjectController = new ProjectController();
        this.BackupController = new BackupController();
        this.SlideController = new SlideController();
        this.UserController = new UserController();
    }

    private SslContextFactory getSslContextFactory() {
        try {
//            SslContextFactory sslContextFactory = new SslContextFactory();
            SslContextFactory sslContextFactory = new SslContextFactory.Server();

            URL path = SecureServer.class.getProtectionDomain().getCodeSource().getLocation();

            sslContextFactory.setKeyStorePath(path.toURI().resolve(Config.getString("ssl.keystore.path")).toASCIIString());
            sslContextFactory.setKeyStorePassword(Config.getString("ssl.keystore.password"));
            return sslContextFactory;
        } catch (URISyntaxException e) {
            logger.error("Couldn't start HTTPS server, no valid keystore.");
            return null;
        }
    }
}
