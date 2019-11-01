package fi.ylihallila.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import org.openslide.OpenSlide;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.util.*;

public class Server {

    private Javalin javalin = Javalin.create().start(7000);

    public Server() {
        javalin.get("/api/v0/properties/:slide", ctx -> {
            OpenSlide osr = getOpenslide(ctx.pathParam("slide"));

            Map<String, String> properties = osr.getProperties();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(properties);

            ctx.result(json);
        });

        javalin.get("/api/v0/render_region/:slide/:tileX/:tileY/:level/:tileWidth/:tileHeight", ctx -> {
            int tileX      = ctx.pathParam("tileX", Integer.class).get();
            int tileY      = ctx.pathParam("tileY", Integer.class).get();
            int level      = ctx.pathParam("level", Integer.class).get();
            int tileWidth  = ctx.pathParam("tileWidth", Integer.class).get();
            int tileHeight = ctx.pathParam("tileHeight", Integer.class).get();

            BufferedImage img = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB_PRE);
            int data[] = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

            OpenSlide osr = getOpenslide(ctx.pathParam("slide"));
            osr.paintRegionARGB(data, tileX, tileY, level, tileWidth, tileHeight);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(img, "png", os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());

            ctx.contentType("image/png");
            ctx.result(is);

            os.flush();
            is.close();
        });
    }

    private OpenSlide getOpenslide(String slide) throws IOException {
        File file = new File("H:\\Opiskelu\\QuPath\\" + slide);
        return new OpenSlide(file);
    }
}
