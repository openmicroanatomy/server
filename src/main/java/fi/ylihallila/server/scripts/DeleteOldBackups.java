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

public class DeleteOldBackups extends Script {

    private final long A_YEAR = TimeUnit.DAYS.toMillis(365);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override String getDescription() {
        return "Remove backups older than a year daily";
    }

    @Override long getInterval() {
        return TimeUnit.DAYS.toSeconds(1);
    }

    @Override public void run() {
        long NOW = System.currentTimeMillis();
        Path backupDirectory = Path.of(Constants.BACKUP_DIRECTORY);

        logger.info("Clearing any old backups ...");
        int deleted = 0;

        try (Stream<Path> files = Files.list(backupDirectory)) {
            for (File file : files.map(Path::toFile).collect(Collectors.toList())) {
                if (file.isDirectory()) {
                    continue;
                }

                if (NOW > (file.lastModified() + A_YEAR)) {
                    Files.delete(file.toPath());
                    deleted++;
                }
            }
        } catch (IOException e) {
            logger.error("Error while deleting old backups", e);
        }

        logger.info("Deleted {} old backups", deleted);
    }
}
