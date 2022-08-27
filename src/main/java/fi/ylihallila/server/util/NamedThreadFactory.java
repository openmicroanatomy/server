package fi.ylihallila.server.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple class used to create thread factories with a custom prefix.
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger threadCount = new AtomicInteger(0);

    public NamedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(@NotNull Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(this.prefix + "-" + this.threadCount.getAndIncrement());
        return thread;
    }
}
