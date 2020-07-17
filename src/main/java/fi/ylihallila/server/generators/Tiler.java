package fi.ylihallila.server.generators;

import fi.ylihallila.server.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Searches the slides directory for slides that are pending upload and submits
 * them to the TileGenerator, which tiles & saves the tiles using
 * the Storage Provider defined in the server configuration.
 */
public class Tiler implements Runnable {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Tiler() {
        run();
    }

    @Override
    public void run() {
        Path path = FileSystems.getDefault().getPath(Constants.SLIDES_DIRECTORY);

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
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
                        logger.info("Submitting {} to tile generation", fileName);

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
            logger.error("Watch Service exception. Tiler unavailable.", e);
        }
//        while (true) {
//            try {
//                List<Path> files = Files.list(Path.of(Constants.SLIDES_DIRECTORY))
//                        .filter(path -> path.endsWith(".pending"))
//                        .collect(Collectors.toList());
//
//                if (files.size() > 0) {
//                    System.out.println("\rFound " + files.size() + " new slides to tile.");
//
//                    for (Path file : files) {
//                        new TileGenerator(file.getFileName().toString());
//                    }
//                } else {
//                    System.out.println("\rNo new slides to tile.");
//                }
//
//                wait(TimeUnit.MINUTES.toMillis(1));
////                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                System.exit(0);
//            }
//        }
    }
}
