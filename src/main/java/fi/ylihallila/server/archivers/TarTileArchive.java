package fi.ylihallila.server.archivers;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * A .tar implementation of TileArchive. This class doesn't utilize any compression methods.
 */
public class TarTileArchive implements TileArchive {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TarArchiveOutputStream tarOs;

    private final File file;

    public TarTileArchive(String slideName, int level) {
        this.file = new File(slideName + "-level-" + level + "-tiles.tar");

        create();
    }

    @Override
    public boolean create() {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            tarOs = new TarArchiveOutputStream(fos);
        } catch (FileNotFoundException e) {
            logger.error("Error while creating Tar archive", e);
            return false;
        }

        return true;
    }

    @Override
    public synchronized boolean addTile(String tileName, byte[] data) {
        try {
            TarArchiveEntry entry = new TarArchiveEntry(tileName);
            entry.setSize(data.length);

            tarOs.putArchiveEntry(entry);

            ByteArrayInputStream is = new ByteArrayInputStream(data);
            IOUtils.copy(is, tarOs);
            is.close();

            tarOs.closeArchiveEntry();
        } catch (IOException e) {
            logger.error("Error while adding {} to archive", tileName, e);
            return false;
        }

        return true;
    }

    @Override
    public File save() {
        try {
            tarOs.close();
        } catch (IOException e) {
            logger.error("Error while saving Tar archive", e);
            return null;
        }

        return file;
    }
}
