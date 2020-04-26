package fi.ylihallila.server;

import fi.ylihallila.server.authentication.Auth;
import fi.ylihallila.server.controllers.*;
import io.javalin.Javalin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.core.security.SecurityUtil.roles;
import static fi.ylihallila.server.authentication.Roles.*;

public class SecureServer {

    private Logger logger = LoggerFactory.getLogger(SecureServer.class);
    private Javalin app = Javalin.create(config -> {
        config.accessManager(Auth::accessManager);
        config.showJavalinBanner = false;

        config.server(() -> {
            Server server = new Server();

            if (Configuration.SECURE_SERVER) {
                config.enforceSsl = true;

                ServerConnector sslConnector = new ServerConnector(server, getSslContextFactory());
                sslConnector.setPort(7000);
                server.addConnector(sslConnector);
            } else {
                ServerConnector connector = new ServerConnector(server);
                connector.setPort(7777);
                server.addConnector(connector);
            }

            return server;
        });
    }).start();

    private UserController UserController = new UserController();
    private WorkspaceController WorkspaceController = new WorkspaceController();
    private ProjectController ProjectController = new ProjectController();
    private SlideController SlideController = new SlideController();


    public SecureServer() {
        /* Authentication */
        app.routes(() -> {
            path("/api/v0/users", () -> {
                get(UserController::getAllUsers, roles(ANYONE));
                post(UserController::createUser, roles(ADMIN));
                get("login", UserController::login, roles(STUDENT, TEACHER, ADMIN));

                path(":username", () -> {
                    get(UserController::getUser, roles(ADMIN));
                    patch(UserController::updateUser, roles(ADMIN));
                    delete(UserController::deleteUser, roles(ADMIN));
                });
            });
        });

        app.routes(() -> path("/api/v0/", () -> {
            /* Slides */
            path("slides", () -> {
                get(SlideController::getAllSlides, roles(ANYONE));

                path(":slide-name", () -> {
                    get(SlideController::getSlideProperties, roles(ANYONE));
                    get("tile/:tileX/:tileY/:level/:tileWidth/:tileHeight",
                            SlideController::renderTile, roles(ANYONE));
                });
            });

            /* Workspaces */
            path("workspaces", () -> {
                get(WorkspaceController::getWorkspaces, roles(ANYONE));
                post(WorkspaceController::createWorkspace, roles(TEACHER, ADMIN));

                path(":workspace-name", () -> {
                    delete(WorkspaceController::deleteWorkspace, roles(TEACHER, ADMIN));
                });
            });

            path("projects", () -> {
                post(ProjectController::createProject, roles(TEACHER, ADMIN));

                path(":project-name", () -> {
                    get(ProjectController::downloadProject, roles(ANYONE));
                    put(ProjectController::updateProject, roles(TEACHER, ADMIN));
                    delete(ProjectController::deleteProject, roles(TEACHER, ADMIN));
                    post(ProjectController::uploadProject, roles(TEACHER, ADMIN));
                });
            });
        }));
    }

    private SslContextFactory getSslContextFactory() {
        try {
            SslContextFactory sslContextFactory = new SslContextFactory();

            URL path = SecureServer.class.getProtectionDomain().getCodeSource().getLocation();

            sslContextFactory.setKeyStorePath(path.toURI().resolve("keystore.jks").toASCIIString());
            sslContextFactory.setKeyStorePassword("qwerty");
            return sslContextFactory;
        } catch (URISyntaxException e) {
            logger.error("Couldn't start HTTPS server, no valid keystore.");
            return null;
        }
    }
}
