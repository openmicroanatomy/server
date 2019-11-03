package fi.ylihallila.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Server {

    private Logger logger = LoggerFactory.getLogger(Server.class);
    private Javalin javalin = Javalin.create().start(7000);

    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final String FILE_FORMAT = "%s_%s_%s_%s_%s_%s.png";
    private final String FOLDER      = "H:\\Opiskelu\\QuPath\\";

    public Server() {
        javalin.get("/api/v0/properties/:slide", ctx -> {
            OpenSlide osr = getOpenslide(ctx.pathParam("slide"));
            Map<String, String> properties = osr.getProperties();

            String json = GSON.toJson(properties);
            ctx.result(json);
        });

        // TODO: Compress PNG images
        javalin.get("/api/v0/render_region/:slide/:tileX/:tileY/:level/:tileWidth/:tileHeight", ctx -> {
            String slide   = ctx.pathParam("slide");
            int tileX      = ctx.pathParam("tileX", Integer.class).get();
            int tileY      = ctx.pathParam("tileY", Integer.class).get();
            int level      = ctx.pathParam("level", Integer.class).get();
            int tileWidth  = ctx.pathParam("tileWidth", Integer.class).get();
            int tileHeight = ctx.pathParam("tileHeight", Integer.class).get();

            logger.info("Requested tile [{},{} / {} / {},{}]", tileX, tileY, level, tileWidth, tileHeight);

            String fileName = String.format(FILE_FORMAT, slide, tileX, tileY, level, tileWidth, tileHeight);
            InputStream is;

            if (Files.exists(Path.of(FOLDER, fileName), LinkOption.NOFOLLOW_LINKS)) {
                logger.info("Retrieving from disk.");
                is = new ByteArrayInputStream(new FileInputStream(new File(FOLDER + fileName)).readAllBytes());
            } else {
                logger.info("Generating image and saving to disk.");
                is = generateImage(slide, tileX, tileY, level, tileWidth, tileHeight, fileName);
            }

            ctx.contentType("image/png");
            ctx.result(is);
            is.close();
        });

        javalin.get("/api/v0/download_workspace", ctx -> {
            // todo
        });

        javalin.get("/api/v0/download_project/:project", ctx -> {
            File file = new File(FOLDER + ctx.pathParam("project") + ".zip");

            if (!file.exists()) {
                ctx.status(404);
                return;
            }

            InputStream is = new ByteArrayInputStream(new FileInputStream(file).readAllBytes());

            ctx.contentType("application/zip");
            ctx.result(is);

            is.close();
        });
    }

    private InputStream generateImage(String slide, int tileX, int tileY, int level, int tileWidth, int tileHeight, String fileName) throws IOException {
        BufferedImage img = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        OpenSlide osr = getOpenslide(slide);
        osr.paintRegionARGB(data, tileX, tileY, level, tileWidth, tileHeight);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "png", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        Files.write(Path.of(FOLDER, fileName),
                os.toByteArray(),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE_NEW);

        os.flush();

        return is;
    }

    // Cache this function? Is this resource intensive
    private OpenSlide getOpenslide(String slide) throws IOException {
        File file = new File(FOLDER + slide);
        return new OpenSlide(file);
    }
}
