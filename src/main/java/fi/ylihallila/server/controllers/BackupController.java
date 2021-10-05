package fi.ylihallila.server.controllers;

import fi.ylihallila.server.commons.Roles;
import fi.ylihallila.server.models.User;
import fi.ylihallila.server.util.Util;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.models.Backup;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.plugin.openapi.annotations.HttpMethod;
import io.javalin.plugin.openapi.annotations.OpenApi;
import io.javalin.plugin.openapi.annotations.OpenApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * TODO: Rewrite backups.
 */
public class BackupController extends Controller {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @OpenApi(
        tags = { "backup" },
        summary = "Fetch all backups",
        method = HttpMethod.GET,
        path = "/api/v0/backups"
    )
    public void getAllBackups(Context ctx) throws IOException {
        Allow(ctx, Roles.ANYONE);
        User user = Authenticator.getUser(ctx);

        if (!(user.hasWriteAccessSomewhere())) {
            throw new UnauthorizedResponse();
        }

        // TODO: Show only projects the user has write access to
        List<Backup> backups = Util.getBackups(backup -> backup.getType().equals(Backup.BackupType.PROJECT));

        ctx.json(backups);
    }

    @OpenApi(
        tags = { "backup" },
        summary = "Restore given backup to specified timestamp",
        pathParams = {
            @OpenApiParam(name = "id", description = "Project ID to restore", required = true),
            @OpenApiParam(name = "timestamp", description = "UNIX Epoch time", required = true),
        },
        method = HttpMethod.GET,
        path = "/api/v0/backups/restore/:id/:timestamp"
    )
    public void restore(Context ctx) throws IOException {
        Allow(ctx, Roles.ANYONE);

        String projectId = ctx.pathParam(":id", String.class).get();
        long timestamp = ctx.pathParam(":timestamp", Long.class).get();

        if (!(hasWritePermission(ctx, projectId))) {
            throw new UnauthorizedResponse();
        }

        List<Backup> backups = Util.getBackups(backup ->
            backup.getFilename().equalsIgnoreCase(projectId) && backup.getTimestamp() == timestamp
        );

        if (backups.size() != 1) {
            throw new NotFoundResponse();
        }

        Backup backup = backups.get(0);
        backup.restore();

        logger.info("Backup {}@{} restored by {}", projectId, timestamp, Authenticator.getUsername(ctx).orElse("Unknown"));
    }
}
