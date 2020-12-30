package fi.ylihallila.server.util;

public class Constants {

    public final static String SLIDE_PROPERTIES_FILE = "slides/%s.properties";
    public final static String PROJECT_FILE_FORMAT   = "projects/%s.json";
    public final static String BACKUP_FILE_FORMAT    = "backups/%s@%s";
    public final static String TILE_LEVELS_FORMAT    = "tiles/%s/%s/";
    public final static String PROJECTS_FOLDER       = "projects/";
    public final static String TILE_FILE_FORMAT      = "tiles/%s/%s/%s_%s_%s_%s.png";
    public final static String WORKSPACE_FILE        = "workspace.json";

    public final static String EDITOR_UPLOADS_FOLDER = "editor-uploads";
    public final static String EDITOR_UPLOADS_URL    = "%s:%s/" + EDITOR_UPLOADS_FOLDER + "/%s";

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
    public final static String ALLAS_URL             = "{host}/{id}/tiles/{level}_{tileX}_{tileY}_{tileWidth}_{tileHeight}.jpg";

    public static boolean SECURE_SERVER;

}
