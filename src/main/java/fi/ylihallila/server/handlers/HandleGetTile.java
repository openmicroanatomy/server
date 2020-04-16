package fi.ylihallila.server.handlers;

import fi.ylihallila.server.Configuration;
import fi.ylihallila.server.OpenSlideCache;
import fi.ylihallila.server.Server;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

// TODO: Compress PNG images
public class HandleGetTile implements Handler {

    private Logger logger = LoggerFactory.getLogger(Server.class);

    @Override
    public void handle(Context ctx) throws Exception {
        String slide   = ctx.pathParam("slide");
        int tileX      = ctx.pathParam("tileX", Integer.class).get();
        int tileY      = ctx.pathParam("tileY", Integer.class).get();
        int level      = ctx.pathParam("level", Integer.class).get();
        int tileWidth  = ctx.pathParam("tileWidth", Integer.class).get();
        int tileHeight = ctx.pathParam("tileHeight", Integer.class).get();

        logger.info("Requested tile [{},{} / {} / {},{}]", tileX, tileY, level, tileWidth, tileHeight);

        String fileName = String.format(Configuration.FILE_FORMAT, slide, tileX, tileY, level, tileWidth, tileHeight);
        InputStream is;

        if (Files.exists(Path.of("tiles", fileName), LinkOption.NOFOLLOW_LINKS)) {
            logger.info("Retrieving from disk.");
            FileInputStream fis = new FileInputStream(Path.of("tiles", fileName).toString());
            is = new ByteArrayInputStream(fis.readAllBytes());
            fis.close();
        } else {
            logger.info("Generating image and saving to disk.");
            is = generateImage(slide, tileX, tileY, level, tileWidth, tileHeight, fileName);
        }

        ctx.contentType("image/jpg");
        ctx.result(is);
        is.close();
    }

    private InputStream generateImage(String slide, int tileX, int tileY, int level, int tileWidth, int tileHeight, String fileName) throws IOException {
        BufferedImage img = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB);
        int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        Optional<OpenSlide> openSlide = OpenSlideCache.get(slide);
        if (openSlide.isEmpty()) {
            return InputStream.nullInputStream();
        }

        openSlide.get().paintRegionARGB(data, tileX, tileY, level, tileWidth, tileHeight);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        Files.write(Path.of("tiles", fileName),
                os.toByteArray(),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE_NEW);

        os.flush();

        return is;
    }
}
