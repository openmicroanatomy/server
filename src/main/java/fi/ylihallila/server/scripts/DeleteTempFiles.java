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

public class DeleteTempFiles extends Script {

    private final long TWO_WEEKS = TimeUnit.DAYS.toMillis(14);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override String getDescription() {
        return "Remove backups older than a two weeks daily";
    }

    @Override long getInterval() {
        return TimeUnit.DAYS.toSeconds(1);
    }

    @Override public void run() {
        long NOW = System.currentTimeMillis();
        Path tempDirectory = Path.of(Constants.TEMP_DIRECTORY);

        logger.info("Deleting temp files ...");
        int deleted = 0;

        try (Stream<Path> files = Files.list(tempDirectory)) {
            for (File file : files.map(Path::toFile).collect(Collectors.toList())) {
                if (file.isDirectory()) {
                    continue;
                }

                if (NOW > (file.lastModified() + TWO_WEEKS)) {
                    Files.delete(file.toPath());
                }
            }
        } catch (IOException e) {
            logger.error("Error while deleting old temporary files", e);
        }

        logger.info("Deleted {} temp files", deleted);
    }
}
