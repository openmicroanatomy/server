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
                ctx.status(422).json(getJsonErrorMessage("Image not uploaded")); return;
            }

            if (!isImage(file.getContent())) {
                ctx.status(422).json(getJsonErrorMessage("Provided file is not an valid image")); return;
            }

            if (file.getSize() > getMaxUploadSizeBytes()) {
                ctx.status(422).json(getJsonErrorMessage("Image too large. Max size " + getMaxUploadSizeHumanReadable())); return;
            }

            String fileName = UUID.randomUUID() + file.getExtension();

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
            ctx.status(500).json(getJsonErrorMessage("Error while uploading file: " + e.getMessage()));
        }
    }

    public void options(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Headers", "Authorization");
    }

    /**
     * Parses the uploads.max.size from the config and return it as bytes.
     * Defaults to {@link Constants#DEFAULT_MAX_UPLOAD_SIZE} if something goes wrong.
     */
    private long getMaxUploadSizeBytes() {
        String uploadMaxSize = Config.getString("uploads.max.size");

        try {
            if (uploadMaxSize.endsWith("M")) {
                return Integer.parseInt(uploadMaxSize.substring(0, uploadMaxSize.length() - 1)) * 1024L * 1024L;
            }
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            logger.warn("Formatting error with `uploads.max.size` in your config ({}). Using the default value of 5 MB.", e.getMessage());
        }

        return Constants.DEFAULT_MAX_UPLOAD_SIZE;
    }

    private String getMaxUploadSizeHumanReadable() {
        return (getMaxUploadSizeBytes() * 1024 * 1024) + "MB";
    }

    /**
     * CKEditor expects JSON to be formatted as <code>{ "error" { "message": "[message]" } }</code>
     */
    public Map<String, Map<String, String>> getJsonErrorMessage(String message) {
        return Map.of("error", Map.of("message", message));
    }
}
