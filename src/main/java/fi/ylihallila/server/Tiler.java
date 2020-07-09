package fi.ylihallila.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Searches slides directory for slides that are pending upload and submits them to the TileGenerator, which
 * tiles & uploads the tiles to Allas Object Storage.
 */
public class Tiler implements Runnable {

    public Tiler() {
        run();
    }

    @Override
    public void run() {
        while (true) {
            try {
                List<Path> files = Files.list(Path.of(Config.SLIDES_DIRECTORY))
                        .filter(path -> path.endsWith(".pending"))
                        .collect(Collectors.toList());

                if (files.size() > 0) {
                    System.out.println("\rFound " + files.size() + " new slides to tile.");

                    for (Path file : files) {
                        new TileGenerator(file.getFileName().toString());
                    }
                } else {
                    System.out.println("\rNo new slides to tile.");
                }

                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(0);
            }
        }
    }
}
