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
            generateTiles(args[1]);
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

    private static void generateTiles(String slideName) throws IOException, InterruptedException {
        System.loadLibrary("openslide-jni");
        OpenSlide openSlide = new OpenSlide(new File(slideName));

        int slideWidth = Integer.parseInt(openSlide.getProperties().get("openslide.level[0].width"));
        int slideHeight = Integer.parseInt(openSlide.getProperties().get("openslide.level[0].height"));

        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        int levels = Integer.parseInt(openSlide.getProperties().get("openslide.level-count"));
        for (int level = 0; level < levels; level++) {
            int tileHeight = Integer.parseInt(openSlide.getProperties().get("openslide.level[" + level + "].tile-height"));
            int tileWidth = Integer.parseInt(openSlide.getProperties().get("openslide.level[" + level + "].tile-width"));

            int rows = (int) Math.ceil(Double.parseDouble(openSlide.getProperties().get("openslide.level[" + level + "].width")) / tileWidth);
            int cols = (int) Math.ceil(Double.parseDouble(openSlide.getProperties().get("openslide.level[" + level + "].height")) / tileHeight);

            for (int row = 0; row <= rows; row++) {
                for (int col = 0; col <= cols; col++) {
                    executor.submit(new TileGenerator(
                            openSlide,
                            level, row, col,
                            tileWidth, tileHeight,
                            slideWidth, slideHeight,
                            slideName
                    ));
                }
            }
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        logger.info("Took " + (endTime - startTime) + "ms to generate tiles.");
    }

    private static class TileGenerator implements Callable<Boolean> {

        private OpenSlide openSlide;

        private int level;
        private int row;
        private int col;

        private int tileWidth;
        private int tileHeight;

        private int slideWidth;
        private int slideHeight;

        private String slideName;

        public TileGenerator(OpenSlide openSlide, int level, int row, int col, int tileWidth, int tileHeight, int slideWidth, int slideHeight, String slideName) {
            this.openSlide = openSlide;
            this.level = level;
            this.row = row;
            this.col = col;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.slideWidth = slideWidth;
            this.slideHeight = slideHeight;
            this.slideName = slideName;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                int downsample = (int) Double.parseDouble(openSlide.getProperties().get("openslide.level[" + level + "].downsample"));

                int tileX = row * tileWidth * downsample;
                int tileY = col * tileHeight * downsample;
                int adjustX = 0;
                int adjustY = 0;

                if ((tileX + downsample * tileWidth) > slideWidth) {
                    adjustX = tileWidth - Math.abs((tileX - slideWidth) / downsample);
                }

                if ((tileY + downsample * tileHeight) > slideHeight) {
                    adjustY = tileHeight - Math.abs((tileY - slideHeight) / downsample);
                }

                BufferedImage img = new BufferedImage(tileWidth - adjustX, tileHeight - adjustY, BufferedImage.TYPE_INT_RGB);
                int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

                openSlide.paintRegionARGB(data, tileX, tileY, level, tileWidth - adjustX, tileHeight - adjustY);

                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", os);
                String fileName = String.format(Configuration.FILE_FORMAT, slideName, tileX, tileY, level, tileWidth - adjustX, tileHeight - adjustY);

                Files.write(Path.of("tiles", fileName),
                        os.toByteArray(),
                        StandardOpenOption.WRITE,
                        StandardOpenOption.CREATE);

                os.flush();

                logger.info("Generating: " + slideName + "_" + tileX + "_" + tileY + "_" + level + "_" + (tileWidth - adjustX) + "_" + (tileHeight - adjustY) + ".png");
            } catch (IOException e) {
                logger.error("Error when generating tile: ", e);
                return false;
            }

            return true;
        }
    }
}
