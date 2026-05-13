package com.tcc.pjb.backend.platform.concurrent;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;

public final class PjbVirtualThreadSpine {

    private PjbVirtualThreadSpine() {
    }

    public static ExecutorService newPerTaskExecutor(String threadNamePrefix) {
        String prefix = sanitizePrefix(threadNamePrefix, "pjb-vt-");
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(prefix, 0).factory());
    }

    public static Thread start(String threadName, Runnable task) {
        Objects.requireNonNull(task, "task");
        String name = sanitizeName(threadName, "pjb-vt-task");
        return Thread.ofVirtual().name(name).start(task);
    }

    public static SimpleAsyncTaskExecutor newAsyncTaskExecutor(SimpleAsyncTaskExecutorBuilder builder,
                                                               String threadNamePrefix,
                                                               int concurrencyLimit,
                                                               boolean rejectWhenSaturated,
                                                               Duration terminationTimeout,
                                                               TaskDecorator taskDecorator) {
        Objects.requireNonNull(builder, "builder");
        SimpleAsyncTaskExecutorBuilder configured = builder
                .virtualThreads(true)
                .threadNamePrefix(sanitizePrefix(threadNamePrefix, "pjb-vt-"))
                .concurrencyLimit(Math.max(1, concurrencyLimit))
                .rejectTasksWhenLimitReached(rejectWhenSaturated)
                .taskTerminationTimeout(sanitizeTimeout(terminationTimeout));
        if (taskDecorator != null) {
            configured = configured.taskDecorator(taskDecorator);
        }
        return configured.build();
    }

    private static String sanitizePrefix(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.endsWith("-") ? normalized : normalized + '-';
    }

    private static String sanitizeName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static Duration sanitizeTimeout(Duration value) {
        if (value == null || value.isNegative() || value.isZero()) {
            return Duration.ofSeconds(30);
        }
        return value;
    }
}
