package fi.ylihallila.server.scripts;

public abstract class Script implements Runnable {

    /**
     * Should describe what the script does and mention its interval in plain english.
     */
    abstract String getDescription();

    /**
     * Initial delay and interval between executions <b>in seconds</b>.
     */
    abstract long getInterval();

}
