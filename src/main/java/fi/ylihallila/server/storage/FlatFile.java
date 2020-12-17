package fi.ylihallila.server.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FlatFile implements StorageProvider {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override public void commitFile(File file) {
        try {
            Files.copy(
                file.toPath(),
                Path.of("tiles", file.getName())
            );
        } catch (IOException e) {
            logger.error("Error while saving tile archive to flat file.");
        }
    }

    @Override public void commitArchive(File file) {
        try {
            Files.copy(
                file.toPath(),
                Path.of("tiles", file.getName())
            );
        } catch (IOException e) {
            logger.error("Error while saving tile archive to flat file.");
        }
    }

    @Override public String getTilesURI() {
        return Paths.get("tiles").toAbsolutePath().toString() + "/{id}/{level}_{tileX}_{tileY}_{tileWidth}_{tileHeight}.jpg";
    }
}
