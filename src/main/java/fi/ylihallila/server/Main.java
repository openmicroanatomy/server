package fi.ylihallila.server;

import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        } else {
            boolean secure = !(args.length == 1 && args[0].equals("--insecure"));

            Files.createDirectories(Path.of("projects"));
            Files.createDirectories(Path.of("slides"));
            Files.createDirectories(Path.of("tiles"));
            Files.createDirectories(Path.of("backups"));

            /*
             * TODO:
             *  * Copy Dummy.zip from resources to disk
             *  * Add possibility to specify domain & port & secure-mode
             */

            try {
                Configuration.SECURE_SERVER = secure;
                new SecureServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
