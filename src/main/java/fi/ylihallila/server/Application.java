package fi.ylihallila.server;

import com.typesafe.config.ConfigFactory;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.controllers.*;
import fi.ylihallila.server.scripts.*;
import fi.ylihallila.server.util.Configuration;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.Database;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.javalin.plugin.rendering.vue.JavalinVue;
import io.swagger.v3.oas.models.info.Info;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;

import static fi.ylihallila.server.commons.Roles.*;
import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.core.security.SecurityUtil.roles;

public class Application {

    private final Logger logger = LoggerFactory.getLogger(Application.class);

    private static final Path configPath = Path.of("application.conf");

    /**
     * Loads the reference config (resources/reference.conf)
     */
    private static final com.typesafe.config.Config baseConfig = ConfigFactory.load();

    private static final Configuration configuration = new Configuration(ConfigFactory.parseFile(configPath.toFile()).withFallback(baseConfig));

    private Javalin javalin = Javalin.create(config -> {
        config.registerPlugin(new OpenApiPlugin(getOpenApiOptions()));
        config.accessManager(Authenticator::accessManager);
        config.showJavalinBanner = false;
        config.maxRequestSize = Long.MAX_VALUE;
        config.addStaticFiles("/logos", Path.of("logos").toAbsolutePath().toString(), Location.EXTERNAL);
        config.addStaticFiles("/tiles", Path.of("tiles").toAbsolutePath().toString(), Location.EXTERNAL);
        config.addStaticFiles("/uploads", Path.of("uploads").toAbsolutePath().toString(), Location.EXTERNAL);
        config.enableCorsForAllOrigins();

        config.server(() -> {
            Server server = new Server();

            // Set the thread prefix to "server-" instead of "qtp-" to improve logging.
            if (server.getThreadPool() instanceof QueuedThreadPool) {
                ((QueuedThreadPool) server.getThreadPool()).setName("server");
            }

            ServerConnector connector = new ServerConnector(server);
            connector.setPort(Constants.SERVER_PORT);
            server.addConnector(connector);

            return server;
        });
    }).start();

    private final OrganizationController   OrganizationController = new OrganizationController();
    private final DashboardController      DashboardController    = new DashboardController();
    private final WorkspaceController      WorkspaceController    = new WorkspaceController();
    private final PasswordController       PasswordController     = new PasswordController();
    private final ProjectController        ProjectController      = new ProjectController();
    private final SubjectController        SubjectController      = new SubjectController();
    private final BackupController         BackupController       = new BackupController();
    private final ServerController         ServerController       = new ServerController();
    private final SlideController          SlideController        = new SlideController();
    private final UserController           UserController         = new UserController();
    private final FileController           FileController         = new FileController();
    private final AuthenticationController AuthController         = new AuthenticationController();

    private final ScriptManager scriptManager = new ScriptManager(
        new BackupDatabase(), new DeleteOldBackups(), new DeleteTempFiles()
    );

    public Application() {
        JavalinVue.rootDirectory("/vue", Location.CLASSPATH);

        javalin.get("/", DashboardController::index, roles(ANYONE));

        javalin.routes(() -> path("/api/v0/", () -> {
            before(ctx -> {
                logger.debug("Creating Database Session for Request");

                Session session = Database.openSession();
                session.beginTransaction();

                ctx.register(Session.class, session);
            });

            after(ctx -> {
                logger.debug("Destroying Database Session for Request");

                Session session = ctx.use(Session.class);

                if (session != null) {
                    if (session.getTransaction() != null) {
                        session.getTransaction().commit();
                    }

                    session.close();
                }
            });

            /* Server */

            get("server", ServerController::get, roles(ANYONE));

            /* Authentication */

            path("auth", () -> {
                get("login", AuthController::login,             roles(ANYONE));
                get("verify", AuthController::verify,           roles(ANYONE));
                get("write/:id", AuthController::hasWritePermission, roles(ANYONE));
                get("read/:id",  AuthController::hasReadPermission,  roles(ANYONE));
            });

            /* Password Recovery */

            path("password", () -> {
                post("set/:token", PasswordController::reset,    roles(ANYONE));
                post("recovery",   PasswordController::recovery, roles(ANYONE));
            });

            /* Users */

            crud("/users/:id", UserController, roles(ANYONE));

            /* Upload */

            post("upload/ckeditor",                FileController::upload,  roles(ANYONE));
            javalin.options("/api/v0/upload/ckeditor", FileController::options, roles(ANYONE));

            /* Slides */

            crud("slides/:id", SlideController, roles(ANYONE));

            /* Workspaces */

            crud("workspaces/:id", WorkspaceController, roles(ANYONE));

            /* Projects */

            crud("projects/:id", ProjectController, roles(ANYONE));
            post("projects/:id", ProjectController::uploadProject, roles(ANYONE));

            /* Subjects */

            crud("/subjects/:id", SubjectController, roles(ANYONE));

            /* Backups */
            path("backups", () -> {
                get(BackupController::getAllBackups, roles(ANYONE));

                get("restore/:id/:timestamp", BackupController::restore, roles(ANYONE));
            });

            /* Organizations */
            crud("/organizations/:id", OrganizationController, roles(ANYONE));
        }));
    }

    /**
     * Gracefully stops Javalin and closes database connection.
     * Waits for up to 60 seconds for any database sessions to close before forcefully exiting.
     */
    public void stop() {
        try {
            javalin.stop();

            SessionFactory factory = Database.getSessionFactory();

            for (int i = 1; i <= 30; i++) {
                if (Database.getCurrentlyOpenSessionsCount() == 0) break;


                logger.info("[{}/{}] Waiting for {} database connections to close ...", i, 30, Database.getCurrentlyOpenSessionsCount());

                Thread.sleep(2000);

                if (i == 30) {
                    logger.info("Database connections not after 30 retries -- exiting forcefully ...");
                }
            }

            logger.info("Closing database ...");

            factory.close();

            logger.info("Database has closed");

            logger.info("Server successfully stopped");

        } catch (Exception e) {
            logger.error("Error while stopping server, stopping anyway ...", e);
        }

        System.exit(0);
    }

    public Javalin getJavalin() {
        return javalin;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    private OpenApiOptions getOpenApiOptions() {
        Info applicationInfo = new Info()
                .version("1.0")
                .description("Open Microanatomy Server");

        OpenApiOptions apiOptions = new OpenApiOptions(applicationInfo)
                .path("/docs")
                .swagger(new SwaggerOptions("/swagger").title("Open Microanatomy Server Documentation"))
                .roles(Set.of(ANYONE));

        return apiOptions;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }
}
