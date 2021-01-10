package fi.ylihallila.server.archivers;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class ZipTileArchive implements TileArchive {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ZipArchiveOutputStream zipOs;

    private final File file;

    public ZipTileArchive(String slideName, int level) {
        this.file = new File(slideName + "-level-" + level + "-tiles.zip");

        create();
    }

    @Override
    public boolean create() {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            zipOs = new ZipArchiveOutputStream(fos);
        } catch (FileNotFoundException e) {
            logger.error("Error while creating Tar archive", e);
            return false;
        }

        return true;
    }

    @Override
    public synchronized boolean addTile(String tileName, byte[] data) {
        try {
            ZipArchiveEntry entry = new ZipArchiveEntry(tileName);
            entry.setSize(data.length);

            zipOs.putArchiveEntry(entry);

            ByteArrayInputStream is = new ByteArrayInputStream(data);
            IOUtils.copy(is, zipOs);
            is.close();

            zipOs.closeArchiveEntry();
        } catch (IOException e) {
            logger.error("Error while adding {} to archive", tileName, e);
            return false;
        }

        return true;
    }

    @Override
    public File save() {
        try {
            zipOs.close();
        } catch (IOException e) {
            logger.error("Error while saving Tar archive", e);
            return null;
        }

        return file;
    }
}
