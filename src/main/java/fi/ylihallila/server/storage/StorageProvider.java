package fi.ylihallila.server.storage;

import java.io.File;

/**
 * Storage Provider represents a place where tiles can be saved & accessed from.
 */
public interface StorageProvider {

    /**
     * Upload a single file.
     *
     * @param file file to upload
     */
    void commitFile(File file);

    /**
     * Upload a archive.
     *
     * @param file archive to upload.
     */
    void commitArchive(File file);

    /**
     * Returns the URI where the tiles are located. Contains placeholders {id}, {level}, {tileX}, {tileY},
     * {tileHeight}, {tileWidth} and any possible Storage Provider specific placeholders.
     *
     * @return string URI with placeholders.
     */
    String getTilesURI();

}
