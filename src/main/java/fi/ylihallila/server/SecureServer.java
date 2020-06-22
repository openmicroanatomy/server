package fi.ylihallila.server;

import fi.ylihallila.server.authentication.Auth;
import fi.ylihallila.server.authentication.Roles;
import fi.ylihallila.server.controllers.*;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.plugin.rendering.vue.JavalinVue;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import io.javalin.plugin.rendering.vue.VueComponent;

import javax.servlet.http.HttpServletRequest;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.core.security.SecurityUtil.roles;
import static fi.ylihallila.server.authentication.Roles.*;

public class SecureServer {

    private Logger logger = LoggerFactory.getLogger(SecureServer.class);
    private Javalin app = Javalin.create(config -> {
        config.accessManager(Auth::accessManager);
        config.showJavalinBanner = false;
        config.enableWebjars();
        config.requestCacheSize = Long.MAX_VALUE;
        config.addStaticFiles("/static", Location.CLASSPATH);

        JavalinVue.rootDirectory("/vue", Location.CLASSPATH);

        config.server(() -> {
            Server server = new Server();

            if (Config.SECURE_SERVER) {
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
        app.get("/", new VueComponent("index"),          roles(TEACHER, ADMIN));
        app.get("/upload", new VueComponent("uploader"), roles(ADMIN));

        /* API */
        app.get("/api/v1/slides", SlideController::GetAllSlidesV2, roles(ANYONE));

        app.routes(() -> path("/api/v0/", () -> {
            /* Authentication */
            path("users", () -> {
                get(UserController::getAllUsers,         roles(ANYONE));
                post(UserController::createUser,         roles(ADMIN));
                get("login", UserController::login, roles(STUDENT, TEACHER, ADMIN));

                path(":username", () -> {
                    get(UserController::getUser,       roles(ADMIN));
                    patch(UserController::updateUser,  roles(ADMIN));
                    delete(UserController::deleteUser, roles(ADMIN));
                });
            });

            /* Upload */
            get("upload", SlideController::upload,  roles(ANYONE));
            post("upload", SlideController::upload, roles(ANYONE));

            /* Slides */
            path("slides", () -> {
                get(SlideController::getAllSlides, roles(ANYONE));

                path(":slide-id", () -> {
                    get(SlideController::getSlideProperties, roles(ANYONE));
                    put(SlideController::updateSlide,        roles(TEACHER, ADMIN));
                    delete(SlideController::deleteSlide,     roles(TEACHER, ADMIN));
                    get("tile/:tileX/:tileY/:level/:tileWidth/:tileHeight", SlideController::renderTile, roles(ANYONE));
                });
            });

            /* Workspaces */
            path("workspaces", () -> {
                get(WorkspaceController::getWorkspaces,    roles(ANYONE));
                post(WorkspaceController::createWorkspace, roles(TEACHER, ADMIN));

                path(":workspace-id", () -> {
                    put(WorkspaceController::updateWorkspace,    roles(TEACHER, ADMIN));
                    delete(WorkspaceController::deleteWorkspace, roles(TEACHER, ADMIN));
                });
            });

            /* Projects */
            path("projects", () -> {
                post(ProjectController::createProject, roles(TEACHER, ADMIN));

                path(":project-id", () -> {
                    get(ProjectController::downloadProject,  roles(ANYONE));
                    put(ProjectController::updateProject,    roles(TEACHER, ADMIN));
                    delete(ProjectController::deleteProject, roles(TEACHER, ADMIN));
                    post(ProjectController::uploadProject,   roles(TEACHER, ADMIN));
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
