package fi.ylihallila.server.gson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fi.ylihallila.server.Config;
import fi.ylihallila.server.Util;
import org.apache.commons.compress.utils.FileNameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Backup {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String filename;
    private long timestamp;
    private String readable;

    private Path filepath;
    private BackupType type;

    public Backup(String filename, long timestamp) {
        this.filename = filename;
        this.timestamp = timestamp;
        this.filepath = Path.of(String.format(Config.BACKUP_FILE_FORMAT, filename, timestamp));
        this.readable = Util.getHumanReadableName(getBaseName()).orElse(filename);

        if (filename.contains(".zip")) {
            this.type = BackupType.PROJECT;
        } else {
            this.type = BackupType.OTHER;
        }
    }

    public String getBaseName() {
        return FileNameUtils.getBaseName(filename);
    }

    public String getFilename() {
        return filename;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getReadable() {
        return readable;
    }

    @JsonIgnore
    public Path getFilepath() {
        return filepath;
    }

    public BackupType getType() {
        return type;
    }

    public void restore() throws IOException {
        logger.debug("Restoring backup {}@{}", filename, timestamp);

        if (type == BackupType.PROJECT) {
            Files.move(
                filepath,
                Path.of(Config.PROJECTS_FOLDER, filename),
                StandardCopyOption.REPLACE_EXISTING
            );
        } else {
            Files.move(
                filepath,
                Path.of(filename),
                StandardCopyOption.REPLACE_EXISTING
            );
        }

        logger.debug("Successfully restored backup.");
    }

    public void delete() throws IOException {
        Files.delete(filepath);
    }

    @Override
    public String toString() {
        return "Backup{" +
                "filename='" + filename + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                '}';
    }

    public enum BackupType {

        PROJECT,
        OTHER

    }
}
