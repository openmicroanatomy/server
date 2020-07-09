package fi.ylihallila.server.archivers;

import java.io.File;

/**
 * Represents a archive of tiles.
 */
public interface TileArchive {

    /**
     * Creates the archive.
     *
     * @return true if success.
     */
    boolean create();

    /**
     * Adds a tile to the archive.
     *
     * @param tileName tile name.
     * @param data data to insert.
     * @return true if success.
     */
    boolean addTile(String tileName, byte[] data);

    /**
     * Saves the archive to disk.
     *
     * @return File archive file.
     */
    File save();

}
