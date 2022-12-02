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
     * Save a byte array as a file.
     *
     * @param bytes Bytes to save.
     * @param fileName Name for file.
     */
    void commitFile(byte[] bytes, String fileName);

    /**
     * Save an archive.
     *
     * @param file archive to save.
     */
    void commitArchive(File file);

    /**
     * Returns the URI where the tiles are located.
     * Supported placeholders: <code>{id}, {level}, {tileX}, {tileY}, {tileHeight}, {tileWidth}</code>.
     *
     * @return string tile URI with placeholders.
     */
    String getTilesURI();

    /**
     * Returns the URI where the thumbnail is located. Supported placeholders: <code>{id}</code>.
     *
     * @return string thumbnail URI with placeholders.
     */
    String getThumbnailURI();

    /**
     * Returns the individual tile naming format. Recommended that the name ends in <b>.jpg</b>.
     * Supported placeholders are: <code>{id}, {level}, {tileX}, {tileY}, {tileHeight}, {tileWidth}</code>.
     *
     * @return string Tile naming scheme with placeholders.
     */
    String getTileNamingFormat();

    /**
     * Returns the thumbnail naming format. Recommended that the name ends in <b>.jpg</b>.
     * Supported placeholders are: <code>{id}</code>.
     *
     * @return string Thumbnail naming scheme with placeholders.
     */
    String getThumbnailNamingFormat();

    /**
     * @return string Friendly name for Storage Provider.
     */
    String getName();
}
