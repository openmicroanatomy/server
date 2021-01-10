package fi.ylihallila.server.util;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Path configPath = Path.of("application.conf");

    // Copy reference config to server directory
    static {
        if (!(Files.exists(configPath))) {
            try {
                Files.copy(Config.class.getResourceAsStream("/reference.conf"), configPath);
            } catch (IOException e) {
                logger.error("Could not copy reference config", e);
            }
        }
    }

    /**
     * Loads the reference config (resources/reference.conf)
     */
    private static final com.typesafe.config.Config baseConfig = ConfigFactory.load();

    public static com.typesafe.config.Config Config = ConfigFactory.parseFile(configPath.toFile()).withFallback(baseConfig);

}
