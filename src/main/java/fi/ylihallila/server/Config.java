package fi.ylihallila.server;

import java.io.File;
import java.nio.file.Path;

public class Config {

    public final static String SLIDE_PROPERTIES_FILE = "slides/%s.properties";
    public final static String PROJECT_FILE_FORMAT   = "projects/%s.zip";
    public final static String BACKUP_FILE_FORMAT    = "backups/%s@%s";
    public final static String TILE_LEVELS_FORMAT    = "tiles/%s/%s/";
    public final static String PROJECTS_FOLDER       = "projects/";
    public final static String TILE_FILE_FORMAT      = "tiles/%s/%s/%s_%s_%s_%s.png";
    public final static String WORKSPACE_FILE        = "workspace.json";

    /**
     * Slides that are pending tiling & upload to <a href="https://docs.csc.fi/data/Allas/">Allas</a>.
     * These files are scanned regularly by the tiler. (@see Tiler)
     */
    public final static String PENDING_SLIDES        = "slides/%s.pending";

    public final static String SLIDES_DIRECTORY      = "slides";

    public final static String BACKUP_FOLDER         = "backups/";
    public final static String UPLOADED_FILE         = "uploads/%s";
    public final static String SLIDES_FILE           = "slides.json";
    public final static String USERS_FILE            = "users.json";
    public final static String ALLAS_URL             = "https://a3s.fi:443/swift/v1/AUTH_692aca385d42495ca2efbae12c3d9366/%s/tiles/{level}_{tileX}_{tileY}_{tileWidth}_{tileHeight}.jpg";

    public static boolean SECURE_SERVER;

    public class Property {

        private String value;

        public Property(String value) {
            this.value = value;
        }

        public String get(String... args) {
            return String.format(value, args);
        }

        public Path asPath(String... args) {
            return Path.of(get(args));
        }

        public File asFile(String... args) {
            return new File(get(args));
        }
    }
}
