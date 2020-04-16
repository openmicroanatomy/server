package fi.ylihallila.server.handlers;

import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class HandleGetProject implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        File file = new File("projects/" + ctx.pathParam("project") + ".zip");

        if (!file.exists()) {
            ctx.status(404);
            return;
        }

        InputStream is = new ByteArrayInputStream(new FileInputStream(file).readAllBytes());

        ctx.contentType("application/zip");
        ctx.result(is);

        is.close();
    }
}
