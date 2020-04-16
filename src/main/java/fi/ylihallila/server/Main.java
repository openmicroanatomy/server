package fi.ylihallila.server;

/**
 * Folder structure
 *
 *  /
 *      server.jar
 *      workspace.json
 *
 *      tiles/
 *          [slide 1]/
 *              [tileX]_[tileY]_[level]_[width]_[height].jpg
*           [slide 2]/
 *  *           [tileX]_[tileY]_[level]_[width]_[height].jpg
 *      projects/
 *          [project 1].zip
 *          [project 2].zip
 *      slides/
 *          [slide 1].svs
 *          [slide 2].svs
 */
public class Main {
    public static void main(String[] args) {
        try {
            new Server();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
