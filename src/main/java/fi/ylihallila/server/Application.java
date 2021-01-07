package fi.ylihallila.server;

import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.controllers.*;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.Database;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.openapi.OpenApiOptions;
import io.javalin.plugin.openapi.OpenApiPlugin;
import io.javalin.plugin.openapi.ui.SwaggerOptions;
import io.swagger.v3.oas.models.info.Info;
import org.apache.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Set;

import static fi.ylihallila.server.commons.Roles.*;
import static fi.ylihallila.server.util.Config.Config;
import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.core.security.SecurityUtil.roles;

public class Application {

    private Logger logger = LoggerFactory.getLogger(Application.class);
    private Javalin app = Javalin.create(config -> {
        config.registerPlugin(new OpenApiPlugin(getOpenApiOptions()));
        config.accessManager(Authenticator::accessManager);
        config.showJavalinBanner = false;
        config.maxRequestSize = Long.MAX_VALUE;
        config.addStaticFiles("/logos", Path.of("organizations").toAbsolutePath().toString(), Location.EXTERNAL);
        config.addStaticFiles("/tiles", Path.of("tiles").toAbsolutePath().toString(), Location.EXTERNAL);
        config.addStaticFiles("/editor-uploads", Path.of("editor-uploads").toAbsolutePath().toString(), Location.EXTERNAL);

        config.server(() -> {
            Server server = new Server();

            // TODO: Allow only secure (production) server in the future.

            if (Constants.SECURE_SERVER) {
                HttpConfiguration httpConfig = new HttpConfiguration();
                httpConfig.setSecureScheme("https");
                httpConfig.setSecurePort(Config.getInt("server.port.secure"));

                SecureRequestCustomizer src = new SecureRequestCustomizer();
                httpConfig.addCustomizer(src);

                HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);
                SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(getSslContextFactory(), HttpVersion.HTTP_1_1.toString());

                ServerConnector sslConnector = new ServerConnector(server,
                    new OptionalSslConnectionFactory(sslConnectionFactory, HttpVersion.HTTP_1_1.toString()),
                    sslConnectionFactory,
                    httpConnectionFactory);
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
    private SubjectController SubjectController;
    private BackupController BackupController;
    private SlideController SlideController;
    private UserController UserController;
    private FileController FileController;

    public Application() {
        createControllers();

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

                if (session != null && session.getTransaction() != null) {
                    session.getTransaction().commit();
                    session.close();
                }
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
            post("upload", SlideController::upload, roles(MANAGE_SLIDES));
            post("upload/ckeditor", FileController::upload, roles(MANAGE_PROJECTS));
            app.options("/api/v0/upload/ckeditor", FileController::options, roles(MANAGE_PROJECTS));

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

            /* Subjects */
            path("subjects", () -> {
                post(SubjectController::createSubject, roles(MANAGE_PROJECTS, MANAGE_PERSONAL_PROJECTS));

                path(":subject-id", () -> {
                   put(SubjectController::updateSubject, roles(MANAGE_PERSONAL_PROJECTS, MANAGE_PROJECTS));
                   delete(SubjectController::deleteSubject, roles(MANAGE_PERSONAL_PROJECTS, MANAGE_PROJECTS));
                });
            });

            /* Backups */
            path("backups", () -> {
                get(BackupController::getAllBackups, roles(MANAGE_PROJECTS));

                get("restore/:file/:timestamp", BackupController::restore, roles(MANAGE_PROJECTS));
            });

            /* Organizations */
            crud("/organizations/:id", OrganizationController, roles(ANYONE));
        }));
    }

    private void createControllers() {
        this.OrganizationController = new OrganizationController();
        this.WorkspaceController = new WorkspaceController();
        this.ProjectController = new ProjectController();
        this.SubjectController = new SubjectController();
        this.BackupController = new BackupController();
        this.SlideController = new SlideController();
        this.UserController = new UserController();
        this.FileController = new FileController();
    }

    private OpenApiOptions getOpenApiOptions() {
        Info applicationInfo = new Info()
                .version("1.0")
                .description("RemoteOpenslide");

        OpenApiOptions apiOptions = new OpenApiOptions(applicationInfo)
                .path("/docs")
                .swagger(new SwaggerOptions("/swagger").title("RemoteOpenslide Documentation"))
                .roles(Set.of(ANYONE));

        return apiOptions;
    }

    private SslContextFactory getSslContextFactory() {
        try {
            SslContextFactory sslContextFactory = new SslContextFactory();

            URL path = Application.class.getProtectionDomain().getCodeSource().getLocation();

            sslContextFactory.setKeyStorePath(path.toURI().resolve(Config.getString("ssl.keystore.path")).toASCIIString());
            sslContextFactory.setKeyStorePassword(Config.getString("ssl.keystore.password"));
            return sslContextFactory;
        } catch (URISyntaxException e) {
            logger.error("Couldn't start HTTPS server, no valid keystore.");
            return null;
        }
    }
}
