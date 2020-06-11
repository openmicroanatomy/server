package fi.ylihallila.server;

public class Configuration {

    public final static String SLIDE_PROPERTIES_FILE = "slides/%s.properties";
    public final static String PROJECT_FILE_FORMAT   = "projects/%s.zip";
    public final static String BACKUP_FILE_FORMAT    = "backups/%s-%s.backup";
    public final static String TILE_FILE_FORMAT      = "tiles/%s/%s_%s_%s_%s_%s.jpg";
    public final static String WORKSPACE_FILE        = "workspace.json";
    public final static String SLIDES_FILE           = "slides.json";

    public static boolean SECURE_SERVER;

}
