package fi.ylihallila.server.storage;

import java.io.File;

/**
 * Storage Provider represents a place where tiles can be saved & accessed from.
 */
public interface StorageProvider {

    /**
     * Save a single file.
     *
     * @param file file to save.
     */
    void commitFile(File file);

    /**
     * Save an archive.
     *
     * @param file archive to save.
     */
    void commitArchive(File file);

    /**
     * Returns the URI where the tiles are located. Contains placeholders {id}, {level}, {tileX}, {tileY},
     * {tileHeight}, {tileWidth} and any possible Storage Provider specific placeholders.
     *
     * @return string URI with placeholders.
     */
    String getTilesURI();

    /**
     * Returns the URI where the thumbnail is located. Contains placeholders {id}.
     *
     * @return string URI with placeholders.
     */
    String getThumbnailURI();

    /**
     * @return string Friendly name for Storage Provider.
     */
    String getName();
}
