package fi.ylihallila.server.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;

public class HandleCreateNewWorkspace implements Handler {

    private Logger logger = LoggerFactory.getLogger(HandleGetWorkspace.class);

    @Override
    public void handle(Context ctx) throws Exception {
        String workspaceToCreate = ctx.pathParam("name");

        File workspaceFile = new File("workspace.json");
        InputStreamReader reader = new InputStreamReader(new FileInputStream(workspaceFile));

        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray workspaces = json.getAsJsonArray("workspaces");

        JsonObject dummyWorkspace = new JsonObject();
        dummyWorkspace.addProperty("name", workspaceToCreate);
        dummyWorkspace.add("projects", new JsonArray());

        workspaces.add(dummyWorkspace);

        logger.info("Creating new workspace: " + workspaceToCreate);
        Files.write(workspaceFile.toPath(), json.toString().getBytes());
    }
}
