package fi.ylihallila.server.controllers;

import fi.ylihallila.server.models.ServerConfiguration;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.jetbrains.annotations.NotNull;

import static fi.ylihallila.server.util.Config.*;

public class ServerController extends Controller {

    @OpenApi(
        summary = "Returns the server configuration",
        tags = { "server" },
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServerConfiguration.class))
    )
    public void get(@NotNull Context ctx) {
        // TODO: Fetch version automatically from gradle.properties?
        ServerConfiguration configuration = new ServerConfiguration(
            "1.0.0-SNAPSHOT",
            Config.getBoolean("auth.guest.enabled"),
            Config.getBoolean("auth.simple.enabled"),
            Config.getBoolean("auth.microsoft.enabled")
        );

        ctx.status(200).json(configuration);
    }
}
