package fi.ylihallila.server.handlers;

import fi.ylihallila.server.Server;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;

public class HandlePostProject implements Handler {

    private Logger logger = LoggerFactory.getLogger(HandlePostProject.class);

    @Override
    public void handle(Context ctx) throws Exception {
        String projectName = ctx.pathParam("project");
        UploadedFile file = ctx.uploadedFile("project");

        if (file == null) {
            logger.debug("Tried to upload file, but didn't exist as form data");
            ctx.status(400);
            return;
        }

        logger.debug("Copying input stream to file");
        copyInputStreamToFile(file.getContent(), new File(Path.of("projects", projectName + ".zip").toUri()));
        logger.debug("Successfully copied file");
        ctx.status(200);
    }

    private void copyInputStreamToFile(InputStream is, File file) throws IOException {
        try (FileOutputStream os = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];

            while ((read = is.read(bytes)) != -1) {
                os.write(bytes, 0, read);
            }
        }
    }
}
