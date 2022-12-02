package fi.ylihallila.server.storage;

import fi.ylihallila.server.util.Constants;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static fi.ylihallila.server.util.Config.Config;

public class FlatFile implements StorageProvider {

    private final String TILE_NAME_FORMAT = "{id}/{level}/{tileX}_{tileY}_{tileWidth}_{tileHeight}.jpg";
    private final String TILE_URL         = "{host}/tiles/" + TILE_NAME_FORMAT;

    private final String THUMBNAIL_NAME_FORMAT = "{id}/thumbnail.jpg";
    private final String THUMBNAIL_URL         = "{host}/tiles/" + THUMBNAIL_NAME_FORMAT;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override public void commitFile(File file) {
        try {
            commitFile(Files.readAllBytes(file.toPath()), file.getName());
        } catch (IOException e) {
            logger.error("Error while saving tile archive to flat file.");
        }
    }

    @Override public void commitFile(byte[] bytes, String fileName) {
        Path tileDirectory = Path.of(Constants.TILE_DIRECTORY);
        Path output = tileDirectory.resolve(fileName);

        try {
            Files.write(output, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void commitArchive(File file) {
        try {
            Path tileDirectory = Path.of(Constants.TILE_DIRECTORY);

            try (TarFile archive = new TarFile(file)) {
                List<TarArchiveEntry> entries = archive.getEntries();

                for (TarArchiveEntry entry : entries) {
                    Path entryPath = tileDirectory.resolve(entry.getName());

                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());

                        try (InputStream in = archive.getInputStream(entry)) {
                            try (OutputStream out = new FileOutputStream(entryPath.toFile())) {
                                IOUtils.copy(in, out);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error while saving tile archive to flat file.", e);
        }
    }

    @Override public String getTilesURI() {
        String host = Config.getString("server.host");

        return TILE_URL.replace("{host}", host);
    }

    @Override public String getThumbnailURI() {
        String host = Config.getString("server.host");

        return THUMBNAIL_URL.replace("{host}", host);
    }

    @Override public String getTileNamingFormat() {
        return TILE_NAME_FORMAT;
    }

    @Override public String getThumbnailNamingFormat() {
        return THUMBNAIL_NAME_FORMAT;
    }

    @Override
    public String getName() {
        return "Local storage";
    }
}
