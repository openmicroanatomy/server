package fi.ylihallila.server.storage;

import fi.ylihallila.server.Config;

import java.io.File;

public class Allas implements StorageProvider {

    @Override
    public void commitFile(File file) {

    }

    @Override
    public void commitArchive(File file) {

    }

    @Override
    public String getTilesURI() {
        return Config.CSC_URL.replace("%s", "{id}");
    }
}
