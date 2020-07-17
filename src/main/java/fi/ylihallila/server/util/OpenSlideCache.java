package fi.ylihallila.server.util;

import org.openslide.OpenSlide;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class OpenSlideCache {

    private static final HashMap<String, OpenSlide> cache = new HashMap<>();

    /**
     * Gets an OpenSlide instance for a given slide. Tries to look
     * for slides where the server.jar file is located.
     *
     * @param filename slide filename
     * @return empty if not found
     * @throws IOException if an I/O error occurs, e.g. when file not found
     */
    public static Optional<OpenSlide> get(String filename) throws IOException {
        if (cache.containsKey(filename)) {
            return Optional.of(cache.get(filename));
        }

        File file = new File(filename);
        if (file.exists()) {
            OpenSlide openSlide = new OpenSlide(file);
            cache.put(filename, openSlide);

            return Optional.of(openSlide);
        }

        return Optional.empty();
    }
}
