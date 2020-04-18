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

public class HandleDeleteProject implements Handler {

    private Logger logger = LoggerFactory.getLogger(HandleGetWorkspace.class);

    @Override
    public void handle(Context ctx) throws Exception {
        File workspaceFile = new File("workspace.json");
        InputStreamReader reader = new InputStreamReader(new FileInputStream(workspaceFile));

        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray workspaces = json.getAsJsonArray("workspaces");

        for (int i = 0; i < workspaces.size(); i++) {
            JsonObject workspace = workspaces.get(i).getAsJsonObject();
            String workspaceName = workspace.get("name").getAsString();

            if (workspaceName.equalsIgnoreCase(ctx.pathParam("workspace"))) {
                deleteProject(workspace, ctx.pathParam("project"));
            }
        }

        Files.write(workspaceFile.toPath(), json.toString().getBytes());
    }

    private void deleteProject(JsonObject workspace, String projectToDelete) {
        JsonArray projects = workspace.get("projects").getAsJsonArray();

        for (int i = 0; i < projects.size(); i++) {
            JsonObject project = projects.get(i).getAsJsonObject();
            String projectName = project.get("name").getAsString();

            if (projectName.equalsIgnoreCase(projectToDelete)) {
                projects.remove(i);
                logger.info("Deleting project: " + projectToDelete);
            }
        }
    }
}
