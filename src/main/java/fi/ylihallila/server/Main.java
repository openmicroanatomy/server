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
 *      users.json
 *      slides.json
 *
 *      keystore.jks
 *      Dummy.zip
 *
 *      tiles/
 *          [slide 1]/
 *              [level]/[tileX]_[tileY]_[width]_[height].jpg
 *          [slide 2]/
 *  *           [level]/[tileX]_[tileY]_[width]_[height].jpg
 *      projects/
 *          [project 1].zip
 *          [project 2].zip
 *      slides/
 *          [slide 1].properties
 *          [slide 2].properties
 *      backups/
 *          [file].[timestamp]
 */
public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length == 2 && args[0].equalsIgnoreCase("--generate")) {
            new TileGenerator(args[1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("--properties")) {
            new PropertiesGenerator(args[1]);
        } else {
            Config.SECURE_SERVER = !(args.length == 1 && args[0].equals("--insecure"));

            Files.createDirectories(Path.of("projects"));
            Files.createDirectories(Path.of("slides"));
            Files.createDirectories(Path.of("tiles"));
            Files.createDirectories(Path.of("backups"));
            Files.createDirectories(Path.of("uploads"));
            Files.createDirectories(Path.of("logos"));

            /*
             * TODO:
             *  * Copy Dummy.zip from resources to disk
             *  * Add possibility to specify domain & port
             */

            try {
                new SecureServer();
            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getClass() == IllegalStateException.class) {
                    logger.info("Add a valid keystore or run in insecure mode with --insecure launch option.");
                }

                logger.error("Launch error", e);
            }
        }
    }
}
