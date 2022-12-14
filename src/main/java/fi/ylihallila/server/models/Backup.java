package fi.ylihallila.server.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.ylihallila.server.util.Constants;
import fi.ylihallila.server.util.Util;
import org.apache.commons.compress.utils.FileNameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Backup {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Filename, including file extension.
     */
    private String filename;

    /**
     * Unix timestamp as milliseconds. Represents when the backup was created.
     */
    private long timestamp;

    /**
     * If backup is of a project, converts the ID to a human readable format.
     */
    private String readable;

    /**
     * Path to backup file.
     */
    private Path filepath;

    /**
     * Type of backup. Only Projects can be restored.
     */
    private BackupType type;

    public Backup(String filename, long timestamp) {
        this.filename = filename;
        this.timestamp = timestamp;
        this.filepath = Path.of(String.format(Constants.BACKUP_FILE_FORMAT, filename, timestamp));
        this.readable = Util.getHumanReadableName(getBaseName());

        if (filename.contains(".json")) {
            this.type = BackupType.PROJECT;
        } else {
            this.type = BackupType.OTHER;
        }
    }

    /**
     * Returns filename without extension.
     */
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

    /**
     * Tries to restore this backup.
     * @throws IOException if an I/O error occurs
     */
    public void restore() throws IOException {
        logger.debug("Restoring backup {}@{}", filename, timestamp);

        if (type == BackupType.PROJECT) {
            Files.move(
                filepath,
                Path.of(Constants.PROJECTS_FOLDER, filename),
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

    /**
     * Deletes the backup file.
     * @throws IOException if an I/O error occurs
     */
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
