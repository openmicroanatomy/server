package fi.ylihallila.server;

import com.google.gson.GsonBuilder;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Folder structure
 *
 *  /
 *
 *      server.jar
 *      workspace.json
 *      keystore.jks
 *      Dummy.zip
 *
 *      tiles/
 *          [slide 1]/
 *              [tileX]_[tileY]_[level]_[width]_[height].jpg
 *          [slide 2]/
 *  *           [tileX]_[tileY]_[level]_[width]_[height].jpg
 *      projects/
 *          [project 1].zip
 *          [project 2].zip
 *      slides/
 *          [slide 1].svs
 *          [slide 2].svs
 */
public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 2 && args[0].equalsIgnoreCase("--generate")) {
            new TileGenerator(args[1]);
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("--properties")) {
            Optional<OpenSlide> openSlide = OpenSlideCache.get(args[1]);

            if (openSlide.isEmpty()) {
                logger.error("Couldn't find slide: " + args[1]);
                return;
            }

            Map<String, String> properties = new HashMap<>(openSlide.get().getProperties());
            properties.put("openslide.remoteserver.uri", "");

            String json = new GsonBuilder().setPrettyPrinting().create().toJson(properties);
            Files.write(Path.of(args[1] + ".properties"), json.getBytes());
            logger.info("Wrote slide properties to file");
        } else {
            Config.SECURE_SERVER = !(args.length == 1 && args[0].equals("--insecure"));

            Files.createDirectories(Path.of("projects"));
            Files.createDirectories(Path.of("slides"));
            Files.createDirectories(Path.of("tiles"));
            Files.createDirectories(Path.of("backups"));
            Files.createDirectories(Path.of("uploads"));

            /*
             * TODO:
             *  * Copy Dummy.zip from resources to disk
             *  * Add possibility to specify domain & port
             */

            try {
                new SecureServer();
            } catch (Exception e) {
                if (e.getCause().getClass() == IllegalStateException.class) {
                    logger.info("Add a valid keystore or run in insecure mode with --insecure launch option.");
                }

                logger.error("Launch error", e);
            }
        }
    }
}
