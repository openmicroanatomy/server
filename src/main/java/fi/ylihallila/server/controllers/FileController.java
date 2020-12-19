package fi.ylihallila.server.controllers;

import fi.ylihallila.server.util.Constants;
import io.javalin.http.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static fi.ylihallila.server.util.Config.Config;

public class FileController extends Controller {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    public void upload(Context ctx) {
        // TODO: Store metadata in database.

        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Headers", "Authorization");

        try {
            UploadedFile file = ctx.uploadedFile("upload");

            if (file == null) {
                ctx.status(400);
                return;
            }

            String fileName = UUID.randomUUID().toString() + file.getExtension();

            FileUtils.copyInputStreamToFile(file.getContent(), new File(Constants.EDITOR_UPLOADS_FOLDER, fileName));

            Map<String, Object> data = new HashMap<>();
            data.put("url",
                String.format(Constants.EDITOR_UPLOADS_URL,
                    Config.getString("server.host"),
                    Config.getString("server.port.insecure"),
                    fileName
                )
            );

            ctx.status(200).json(data);
        } catch (IOException e) {
            logger.error("Error while uploading file", e);
            ctx.status(500);
        }
    }

    public void options(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Headers", "Authorization");
    }
}
