package fi.ylihallila.server.scripts;

import fi.ylihallila.server.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO: Disabled until tiler memory leak is fixed.
 */
public class DeletePendingSlides extends Script {

    private final long TWO_WEEKS = TimeUnit.DAYS.toMillis(14);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override String getDescription() {
        return "Remove slides whose upload has failed daily (i.e. older than 14 days)";
    }

    @Override long getInterval() {
        return TimeUnit.DAYS.toSeconds(1);
    }

    @Override public void run() {
        long NOW = System.currentTimeMillis();
        Path slidesDirectory = Path.of(Constants.SLIDES_DIRECTORY);

        logger.info("Clearing any pending slides ...");
        int deleted = 0;

        try (Stream<Path> files = Files.list(slidesDirectory)) {
            for (File file : files.map(Path::toFile).collect(Collectors.toList())) {
                if (file.isDirectory()) {
                    continue;
                }

                if (!(file.getName().endsWith(".pending"))) {
                    continue;
                }

                if (NOW > (file.lastModified() + TWO_WEEKS)) {
                    Files.delete(file.toPath());

                }
            }
        } catch (IOException e) {
            logger.error("Error while deleting old slides", e);
        }

        logger.info("Deleted {} pending slides", deleted);
    }
}
