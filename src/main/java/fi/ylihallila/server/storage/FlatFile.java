package fi.ylihallila.server.storage;

import fi.ylihallila.server.util.Config;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;

import static fi.ylihallila.server.util.Config.Config;

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
            Path archivePath = Path.of("tiles", file.getName());
            Path tilePath    = Path.of("tiles", FileNameUtils.getBaseName(file.getName()));

            Files.copy(
                file.toPath(),
                archivePath
            );

            ZipFile zipFile = new ZipFile(archivePath.toFile());
            Enumeration<? extends ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                Path entryPath = tilePath.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());

                    try (InputStream in = zipFile.getInputStream(entry)){
                        try (OutputStream out = new FileOutputStream(entryPath.toFile())){
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }

            Files.delete(archivePath);
        } catch (IOException e) {
            logger.error("Error while saving tile archive to flat file.");
        }
    }

    @Override public String getTilesURI() {
        return Config.getString("server.host") + ":" + Config.getString("server.port.insecure") + "/tiles/{id}-level-{level}-tiles/{level}_{tileX}_{tileY}_{tileWidth}_{tileHeight}.jpg";
    }
}
