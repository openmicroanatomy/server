package fi.ylihallila.server.util;

import com.typesafe.config.ConfigFactory;

import java.io.File;

public class Config {

    /**
     * Because ConfigFactory.parseFile(...); does not include the reference.conf, we need to obtain
     * the reference config with ConfigFactory.load(); and provide that as a fallback.
     */
    private static final com.typesafe.config.Config baseConfig = ConfigFactory.load();
    public static com.typesafe.config.Config Config = ConfigFactory.parseFile(new File("application.conf")).withFallback(baseConfig);

}
