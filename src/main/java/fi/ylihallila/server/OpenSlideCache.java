package fi.ylihallila.server;

import org.openslide.OpenSlide;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class OpenSlideCache {

    private static HashMap<String, OpenSlide> cache = new HashMap<>();

    public static Optional<OpenSlide> get(String slide) throws IOException {
        if (cache.containsKey(slide)) {
            return Optional.of(cache.get(slide));
        }

        File file = new File(slide);
        if (file.exists()) {
            OpenSlide openSlide = new OpenSlide(file);
            cache.put(slide, openSlide);

            return Optional.of(openSlide);
        }

        return Optional.empty();
    }
}
