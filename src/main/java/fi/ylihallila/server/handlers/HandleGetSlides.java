package fi.ylihallila.server.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.io.File;

public class HandleGetSlides implements Handler {

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void handle(Context ctx) {
        File directory = new File("slides");

        if (directory.isDirectory()) {
            String[] files = directory.list();

            String json = GSON.toJson(files);
            ctx.result(json);
        } else {
            ctx.status(404);
        }
    }
}
