package fi.ylihallila.server.storage;

import java.io.File;

// TODO: Implement
public class FlatFile implements StorageProvider {

    @Override public void commitFile(File file) {
        // Copy file
    }

    @Override public void commitArchive(File file) {
        // Extract Zip
    }

    @Override public String getTilesURI() {
        // Return file path to tiles/...
        return null;
    }
}
