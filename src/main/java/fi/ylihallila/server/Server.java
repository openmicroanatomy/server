package fi.ylihallila.server;

import fi.ylihallila.server.handlers.*;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

    private Logger logger = LoggerFactory.getLogger(Server.class);
    private Javalin javalin = Javalin.create().start(7000);

    public Server() {
        javalin.get("/api/v0/list_slides", new HandleGetSlides());
        javalin.get("/api/v0/properties/:slide", new HandleGetSlideProperties());
        javalin.get("/api/v0/render_region/:slide/:tileX/:tileY/:level/:tileWidth/:tileHeight", new HandleGetTile());
        javalin.get("/api/v0/download_workspace", new HandleGetWorkspace());
        javalin.get("/api/v0/create_workspace/:name", new HandleCreateNewWorkspace());
        javalin.get("/api/v0/download_project/:project", new HandleGetProject());
        javalin.post("/api/v0/upload_project/:project", new HandlePostProject());
    }
}
