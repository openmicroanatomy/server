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

public class HandleEditProjectDescription implements Handler {

	private Logger logger = LoggerFactory.getLogger(HandleEditProjectDescription.class);


	@Override
	public void handle(Context ctx) throws Exception {
		String projectName = ctx.pathParam("project");
		String description = ctx.formParam("description");

		if (description == null) {
			ctx.status(404);
			return;
		}

		File workspaceFile = new File("workspace.json");
		InputStreamReader reader = new InputStreamReader(new FileInputStream(workspaceFile));

		JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
		JsonArray workspaces = json.getAsJsonArray("workspaces");

		for (int i = 0; i < workspaces.size(); i++) {
			JsonObject workspace = workspaces.get(i).getAsJsonObject();
			JsonArray projects = workspace.get("projects").getAsJsonArray();

			for (int j = 0; j < projects.size(); j++) {
				JsonObject project = projects.get(j).getAsJsonObject();

				if (project.get("name").getAsString().equalsIgnoreCase(projectName)) {
					project.addProperty("description", description);
				}
			}
		}

		logger.info("Editing description for: " + projectName);
		Files.write(workspaceFile.toPath(), json.toString().getBytes());
	}
}
