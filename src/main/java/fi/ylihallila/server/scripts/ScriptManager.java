package fi.ylihallila.server.scripts;

import fi.ylihallila.server.util.NamedThreadFactory;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class ScriptManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("scripts"));

    private final List<ScheduledFuture<?>> scheduledScripts = Lists.newArrayList();

    public ScriptManager(Script... scripts) {
        for (Script script : scripts) {
            scheduleScript(script);
        }
    }

    /**
     * Schedules a new script. The script will run after {@link Script#getInterval()}
     * seconds and then repeatedly every interval.
     */
    public ScheduledFuture<?> scheduleScript(Script script) {
        var scheduledFuture = executor.scheduleAtFixedRate(script, script.getInterval(), script.getInterval(), TimeUnit.SECONDS);
        scheduledScripts.add(scheduledFuture);

        logger.info("Scheduled script: {}", script.getDescription());

        return scheduledFuture;
    }

    public List<ScheduledFuture<?>> getScheduledScripts() {
        return scheduledScripts;
    }

    public ScheduledExecutorService getExecutorService() {
        return executor;
    }
}
