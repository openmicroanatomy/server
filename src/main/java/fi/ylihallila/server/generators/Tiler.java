package fi.ylihallila.server.generators;

import fi.ylihallila.server.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Searches the slides directory for slides that are pending upload and submits
 * them to the TileGenerator, which tiles & saves the tiles using
 * the Storage Provider defined in the server configuration.
 */
public class Tiler implements Runnable {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Tiler() {
        // Sets the logger process to tiler so we log into tiler.log
        MDC.put("process", "tiler");

        run();
    }

    @Override
    public void run() {
        checkForPendingSlides();

        registerWatchService();
    }

    private void checkForPendingSlides() {
        try {
            logger.info("Checking for pending slides ...");

            List<Path> files = Files.list(Path.of(Constants.PENDING_DIRECTORY).toAbsolutePath())
                    .filter(path -> path.toString().endsWith(".pending"))
                    .collect(Collectors.toList());

            if (files.size() == 0) {
                logger.info("No pending slides.");
                return;
            }

            logger.info("Found " + files.size() + " pending slides, adding to queue.");

            for (Path file : files) {
                logger.info("Adding {} to queue.", file.getFileName());

                executor.submit(() -> {
                    try {
                        new TileGenerator(file);
                    } catch (IOException | InterruptedException e) {
                        logger.error("Error while generating tiles for {}", file.getFileName(), e);
                    }
                });
            }
        } catch (IOException e) {
            logger.error("Error while checking for pending slides", e);
        }
    }

    private void registerWatchService() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Path path = FileSystems.getDefault().getPath(Constants.PENDING_DIRECTORY);
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            logger.info("Waiting for slides ...");

            var valid = true;
            while (valid) {
                WatchKey wk = watchService.take();

                for (WatchEvent<?> event : wk.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path dir = (Path) wk.watchable();
                    Path changed = (Path) event.context();
                    String fileName = changed.getFileName().toString();

                    if (fileName.endsWith(".pending")) {
                        System.out.println();
                        logger.info("Found new slide {}. Added to generation queue.", fileName);

                        executor.submit(() -> {
                            try {
                                new TileGenerator(dir.resolve(changed));
                            } catch (IOException | InterruptedException e) {
                                logger.error("Error while generating tiles for {}", fileName, e);
                            }
                        });
                    }
                }

                valid = wk.reset();
                if (!valid) {
                    logger.error("WatchKey has been unregistered. Please restart the tiler instance.");
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.error("WatchService error, tiler unavailable.", e);
        }
    }
}
