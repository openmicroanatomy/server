package fi.ylihallila.server.util;

public class Constants {

    public final static String PERSONAL_WORKSPACE_NAME = "Personal Workspace";
    public final static String COPIED_PROJECTS_NAME    = "Copied Projects";

    public final static String SLIDE_PROPERTIES_FILE = "slides/%s.properties";
    public final static String PROJECT_FILE_FORMAT   = "projects/%s.json";
    public final static String CONFIGURATION_FILE    = "application.conf";
    public final static String BACKUP_FILE_FORMAT    = "backups/%s@%s";
    public final static String PROJECTS_FOLDER       = "projects/";
    public final static String TILE_FILE_FORMAT      = "tiles/%s/%s/%s_%s_%s_%s.png";

    public final static String ORGANIZATION_LOGOS    = "logos/%s.png";

    public final static String EDITOR_UPLOADS_FOLDER = "uploads";
    public final static String EDITOR_UPLOADS_URL    = "%s/" + EDITOR_UPLOADS_FOLDER + "/%s";

    public final static Long DEFAULT_MAX_UPLOAD_SIZE = 5 * 1024L * 1024L; // 2 MB

    /**
     * Slides that are pending tiling & upload to a {@link fi.ylihallila.server.storage.StorageProvider StorageProvider}.
     * These files are scanned regularly by the {@link fi.ylihallila.server.generators.Tiler Tiler}
     */
    public final static String PENDING_DIRECTORY     = "pending/";
    public final static String PENDING_SLIDES        = PENDING_DIRECTORY + "%s.pending";

    public final static String BACKUP_DIRECTORY      = "backups/";

    public final static String DATABASE_BACKUP_FORMAT = BACKUP_DIRECTORY + "database-%s-%s-%s-%s.zip";

    public final static String TEMP_DIRECTORY        = "temp";
    public final static String TEMP_FILE             = "temp/%s";
    public final static String ADMINISTRATORS_FILE   = "administrators.json";

    public final static String PASSWORD_RESET_URL = "%s/api/v0/auth/password/set/%s";

    /* The following constants are set at runtime during server launch */

    public static boolean ENABLE_SSL;
    public static int     SERVER_PORT;

}
