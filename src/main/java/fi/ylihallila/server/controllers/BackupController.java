package fi.ylihallila.server.controllers;

import fi.ylihallila.server.Util;
import fi.ylihallila.server.authentication.Authenticator;
import fi.ylihallila.server.gson.Backup;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class BackupController extends BasicController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void getAllBackups(Context ctx) throws IOException {
        List<Backup> backups = Util.getBackups(backup -> backup.getType().equals(Backup.BackupType.PROJECT));

        ctx.json(backups);
    }

    public void restore(Context ctx) throws IOException {
        String filename = ctx.pathParam(":file", String.class).get();
        long timestamp = ctx.pathParam(":timestamp", Long.class).get();

        List<Backup> backups = Util.getBackups(backup ->
            backup.getFilename().equalsIgnoreCase(filename) && backup.getTimestamp() == timestamp
        );

        if (backups.size() != 1) {
            ctx.status(404);
            return;
        }

        Backup backup = backups.get(0);

        if (!hasPermission(ctx, backup.getBaseName())) {
            throw new UnauthorizedResponse();
        }

        backup.restore();
        logger.info("Backup {}@{} restored by {}", filename, timestamp, Authenticator.getUsername(ctx).orElse("Unknown"));
    }
}
