package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Application;
import fi.ylihallila.server.models.ServerConfiguration;
import fi.ylihallila.server.util.Configuration;
import io.javalin.http.Context;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiContent;
import io.javalin.plugin.openapi.annotations.OpenApiResponse;
import org.jetbrains.annotations.NotNull;

public class ServerController extends Controller {

    @OpenApi(
        summary = "Returns the server configuration",
        tags = { "server" },
        responses = @OpenApiResponse(status = "200", content = @OpenApiContent(from = ServerConfiguration.class))
    )
    public void get(@NotNull Context ctx) {
        Configuration config = Application.getConfiguration();

        // TODO: Fetch version automatically from gradle.properties?
        ServerConfiguration configuration = new ServerConfiguration(
            "1.0.0-SNAPSHOT",
            config.guestsAllowed(),
            config.basicAuthenticationEnabled(),
            config.microsoftAuthenticationEnabled()
        );

        ctx.status(200).json(configuration);
    }
}
