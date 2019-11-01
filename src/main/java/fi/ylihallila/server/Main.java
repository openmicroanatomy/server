package fi.ylihallila.server;

public class Main {
    public static void main(String[] args) {
        try {
            System.loadLibrary("openslide-jni");

            new Server();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
