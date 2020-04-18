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

public class HandleCreateProject implements Handler {

    private Logger logger = LoggerFactory.getLogger(HandleGetWorkspace.class);

    @Override
    public void handle(Context ctx) throws Exception {
        String targetWorkspace = ctx.pathParam("workspace");
        String projectName = ctx.pathParam("project");

        File workspaceFile = new File("workspace.json");
        InputStreamReader reader = new InputStreamReader(new FileInputStream(workspaceFile));

        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray workspaces = json.getAsJsonArray("workspaces");

        for (int i = 0; i < workspaces.size(); i++) {
            JsonObject workspace = workspaces.get(i).getAsJsonObject();
            String workspaceName = workspace.get("name").getAsString();

            if (workspaceName.equalsIgnoreCase(targetWorkspace)) {
                JsonObject project = new JsonObject(); // todo: URL escape & clean-up etc.
                project.addProperty("id", projectName); // must be clean
                project.addProperty("name", projectName); // can contain spaces & formatting
                project.addProperty("description", "");
                project.addProperty("thumbnail", "");
                project.addProperty("server", "");

                workspace.getAsJsonArray("projects").add(project);
                logger.info("Created new project: " + projectName);
            }
        }

        Files.write(workspaceFile.toPath(), json.toString().getBytes());
    }
}
