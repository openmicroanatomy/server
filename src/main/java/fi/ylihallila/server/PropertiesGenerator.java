package fi.ylihallila.server;

import com.google.gson.GsonBuilder;
import org.openslide.OpenSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PropertiesGenerator {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PropertiesGenerator(String slideName) {
        try {
            Optional<OpenSlide> openSlide = OpenSlideCache.get(slideName);

            if (openSlide.isEmpty()) {
                logger.error("Couldn't find slide: " + slideName);
                return;
            }

            Map<String, String> properties = new HashMap<>(openSlide.get().getProperties());
            properties.put("openslide.remoteserver.uri", "");

            String json = new GsonBuilder().setPrettyPrinting().create().toJson(properties);
            Files.write(Path.of(slideName + ".properties"), json.getBytes());

            logger.info("Wrote slide properties to file");
        } catch (IOException e) {
            logger.error("Error while writing slide properties to file", e);
        }
    }
}
