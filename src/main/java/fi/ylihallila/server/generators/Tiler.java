package fi.ylihallila.server.generators;

import fi.ylihallila.server.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Searches the slides directory for slides that are pending upload and submits
 * them to the TileGenerator, which tiles & saves the tiles using
 * the Storage Provider defined in the server configuration.
 */
public class Tiler {

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Tiler() {
        checkForPendingSlides();
    }

    /**
     * Add provided slide to tile generation queue. Does not check if the slide is already queued.
     * @param path to slide
     */
    public void addSlideToTilerQueue(Path path) {
        File file = path.toFile();

        if (!(file.exists())) {
            logger.debug("Tried to add slide to tiler queue, but file did not exist.");
            return;
        }

        if (!(file.canRead())) {
            logger.debug("Tried to add slide to tiler queue, but did not have read permission.");
            return;
        }

        executor.execute(new TileGenerator(file));
    }

    /**
     * Tries to check whether this file is already queued for tiling. This does not work if the slide is already
     * being tiled, thus is being executed and isn't in the queue.
     * @param path to slide being tiled.
     * @return true if queued already.
     */
    public boolean isAlreadyQueued(Path path) {
        File file = path.toFile();

        var isQueued = executor.getQueue()
            .stream()
            .anyMatch(runnable -> {
                if (runnable instanceof TileGenerator generator) {
                    return generator.getSlideFile().equals(file);
                }

                return false;
            });

        return isQueued;
    }

    /**
     * Checks for any slides that are pending to be tiled and prints a warning.
     */
    private void checkForPendingSlides() {
        try (var paths = Files.list(Path.of(Constants.PENDING_DIRECTORY).toAbsolutePath())) {
            logger.info("Checking for slides pending tiling ...");

            var files = paths.filter(path -> path.toString().endsWith(".pending"))
                        .toList();

            if (files.size() > 0) {
                logger.info("Found " + files.size() + " slides, which are pending to be tiled.");
            }
        } catch (IOException e) {
            logger.error("Error while checking for pending slides pending tiling", e);
        }
    }
}
