package fi.ylihallila.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fi.ylihallila.server.util.Config.Config;

/**
 * Small utility class to debug various aspects.
 */
public class SimpleDebugger {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SimpleDebugger() {
        debugConfig();
        debugJavalin();
    }

    /**
     * Prints all configuration values.
     */
    private void debugConfig() {
        logger.debug("Printing all config values");
        logger.debug("=============================================================================");
        logger.debug("server.host: " + Config.getString("server.host"));
        logger.debug("server.port.secure: " + Config.getInt("server.port.secure"));
        logger.debug("server.port: " + Config.getInt("server.port.insecure"));
        logger.debug("auth.guest.enabled: " + Config.getString("auth.guest.enabled"));
        logger.debug("auth.simple.enabled: " + Config.getString("auth.simple.enabled"));
        logger.debug("auth.microsoft.enabled: " + Config.getString("auth.microsoft.enabled"));
        logger.debug("roles.manage.personal.projects.default: " + Config.getBoolean("roles.manage.personal.projects.default"));
        logger.debug("ssl.keystore: " + Config.getString("ssl.keystore.path"));
        logger.debug("ssl.keystore.password: " + Config.getString("ssl.keystore.password"));
        logger.debug("storage.provider: " + Config.getString("storage.provider"));
        logger.debug("allas.username: " + Config.getString("allas.username"));
        logger.debug("allas.password: " + Config.getString("allas.password"));
        logger.debug("allas.auth.url: " + Config.getString("allas.auth.url"));
        logger.debug("allas.domain: " + Config.getString("allas.domain"));
        logger.debug("allas.tenant.id: " + Config.getString("allas.tenant.id"));
        logger.debug("allas.tenant.name: " + Config.getString("allas.tenant.name"));
        logger.debug("flatfile.directory: " + Config.getString("flatfile.directory"));
        logger.debug("app.id: " + Config.getString("app.id"));
        logger.debug("jwk.provider: " + Config.getString("jwk.provider"));
        logger.debug("=============================================================================");
    }

    /**
     * Tests if Javalin is able to launch, mainly checks if the socket is already in use.
     */
    private void debugJavalin() {

    }
}
