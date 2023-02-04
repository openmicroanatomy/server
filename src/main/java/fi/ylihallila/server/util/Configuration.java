package fi.ylihallila.server.util;

import com.typesafe.config.Config;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * A typesafe wrapper for configuration, which also validates and tries to correct some errors in configuration.
 */
public class Configuration {

    private final Config config;

    public Configuration(Config config) {
        this.config = config;
    }

    public String host() {
        String host = config.getString("server.host");

        return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }

    public boolean guestsAllowed() {
        return config.getBoolean("auth.guest.enabled");
    }

    public boolean basicAuthenticationEnabled() {
        return config.getBoolean("auth.basic.enabled");
    }

    public boolean microsoftAuthenticationEnabled() {
        return config.getBoolean("auth.microsoft.enabled");
    }

    public String uploadMaxSize() {
        return config.getString("uploads.max.size");
    }

    public int tileCompression() {
        int compression = config.getInt("tiler.compression");

        return Math.min(100, Math.max(1, compression));
    }

    public String storageProvider() {
        return config.getString("storage.provider").toLowerCase();
    }

    public SMTPConfiguration smtp() {
        return new SMTPConfiguration(config.getConfig("smtp"));
    }

    public AllasConfiguration allas() {
        return new AllasConfiguration(config.getConfig("allas"));
    }

    public MicrosoftConfiguration microsoft() {
        return new MicrosoftConfiguration(config.getConfig("microsoft"));
    }

    public class AllasConfiguration {

        private Config config;

        public AllasConfiguration(Config config) {
            this.config = config;
        }

        public String username() {
            return config.getString("username");
        }

        public String password() {
            return config.getString("password");
        }

        public String authUrl() {
            return config.getString("url");
        }

        public String domain() {
            return config.getString("domain");
        }

        public String tenantId() {
            return config.getString("id");
        }

        public String tenantName() {
            return config.getString("name");
        }
    }

    public class SMTPConfiguration {

        private Config config;

        public SMTPConfiguration(Config config) {
            this.config = config;
        }

        public boolean tls() {
            return config.getBoolean("tls");
        }

        public String host() {
            return config.getString("host");
        }

        public int port() {
            return config.getInt("port");
        }

        public String trusted() {
            return config.getString("ssl.trust");
        }

        public String email() {
            return config.getString("email");
        }

        public String username() {
            return config.getString("username");
        }
        public String password() {
            return config.getString("password");
        }
    }

    public class MicrosoftConfiguration {

        private Config config;

        public MicrosoftConfiguration(Config config) {
            this.config = config;
        }

        public String appId() {
            return config.getString("app.id");
        }

        public String jwkProvider() {
            return config.getString("jwk.provider");
        }
    }
}
