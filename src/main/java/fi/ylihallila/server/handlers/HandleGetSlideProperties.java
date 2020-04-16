package fi.ylihallila.server.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.ylihallila.server.OpenSlideCache;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.openslide.OpenSlide;

import java.util.Map;
import java.util.Optional;

public class HandleGetSlideProperties implements Handler {

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void handle(Context ctx) throws Exception {
        Optional<OpenSlide> openSlide = OpenSlideCache.get(ctx.pathParam("slide"));

        if (openSlide.isEmpty()) {
            ctx.status(404);
            return;
        }

        Map<String, String> properties = openSlide.get().getProperties();

        String json = GSON.toJson(properties);
        ctx.result(json);
    }
}
