package fi.ylihallila.server.handlers;

import fi.ylihallila.server.Server;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class HandleGetWorkspace implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        File file = new File("workspace.json");

        if (!file.exists()) {
            ctx.status(404);
            return;
        }

        InputStream is = new ByteArrayInputStream(new FileInputStream(file).readAllBytes());

        ctx.status(200);
        ctx.contentType("text/plain");
        ctx.result(is);

        is.close();
    }
}
