package com.tcc.pjb.backend.platform.runtime;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.lang.NonNull;

public final class PjbBoundedExecutorService extends AbstractExecutorService implements AutoCloseable {

    private final ExecutorService delegate;
    private final String threadNamePrefix;
    private final int concurrencyLimit;
    private final Semaphore permits;
    private final boolean rejectWhenSaturated;
    private final Duration terminationTimeout;
    private final Duration saturationAcquireTimeout;
    private final LongAdder submittedTasks = new LongAdder();
    private final LongAdder completedTasks = new LongAdder();
    private final LongAdder rejectedTasks = new LongAdder();
    private final LongAdder saturationRejections = new LongAdder();
    private final LongAdder totalAcquireWaitNanos = new LongAdder();
    private final LongAdder acquireAttempts = new LongAdder();
    private final AtomicInteger maxObservedActiveTasks = new AtomicInteger();
    private final AtomicInteger consecutiveRejections = new AtomicInteger();
    private final AtomicLong lastRejectedAtEpochMillis = new AtomicLong();
    private final AtomicLong lastCompletedAtEpochMillis = new AtomicLong();
    private final AtomicBoolean acceptingTasks = new AtomicBoolean(true);
    private volatile boolean shutdown;

    public PjbBoundedExecutorService(String threadNamePrefix,
                                     int concurrencyLimit,
                                     boolean rejectWhenSaturated,
                                     Duration terminationTimeout,
                                     Duration saturationAcquireTimeout) {
        String prefix = threadNamePrefix == null || threadNamePrefix.isBlank() ? "pjb-vt-" : threadNamePrefix;
        int limit = Math.max(1, concurrencyLimit);
        this.threadNamePrefix = prefix;
        this.concurrencyLimit = limit;
        this.delegate = com.tcc.pjb.backend.platform.concurrent.PjbVirtualThreadSpine.newPerTaskExecutor(prefix);
        this.permits = new Semaphore(limit, true);
        this.rejectWhenSaturated = rejectWhenSaturated;
        this.terminationTimeout = sanitizeTimeout(terminationTimeout, Duration.ofSeconds(30));
        this.saturationAcquireTimeout = sanitizeTimeout(saturationAcquireTimeout, rejectWhenSaturated ? Duration.ofMillis(250) : Duration.ofSeconds(2));
    }

    @Override
    public void execute(@NonNull Runnable command) {
        Objects.requireNonNull(command, "command");
        if (!acceptingTasks.get() || shutdown) {
            recordRejection(false);
            throw new RejectedExecutionException("executor is not accepting tasks");
        }
        acquirePermit();
        submittedTasks.increment();
        updateMaxObservedActiveTasks();
        try {
            delegate.execute(() -> {
                try {
                    command.run();
                } finally {
                    completedTasks.increment();
                    consecutiveRejections.set(0);
                    lastCompletedAtEpochMillis.set(System.currentTimeMillis());
                    permits.release();
                }
            });
        } catch (RuntimeException ex) {
            recordRejection(false);
            permits.release();
            throw ex;
        }
    }

    private void acquirePermit() {
        long startedAt = System.nanoTime();
        try {
            if (!rejectWhenSaturated) {
                permits.acquire();
                recordAcquireWait(startedAt);
                return;
            }
            long timeoutMillis = Math.max(1L, saturationAcquireTimeout.toMillis());
            boolean acquired = permits.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
            recordAcquireWait(startedAt);
            if (!acquired) {
                recordRejection(true);
                throw new RejectedExecutionException("executor saturated");
            }
        } catch (InterruptedException ex) {
            recordAcquireWait(startedAt);
            recordRejection(false);
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("interrupted while waiting for executor permit", ex);
        }
    }

    private void recordAcquireWait(long startedAt) {
        long waitedNanos = Math.max(0L, System.nanoTime() - startedAt);
        totalAcquireWaitNanos.add(waitedNanos);
        acquireAttempts.increment();
    }

    private void recordRejection(boolean saturation) {
        rejectedTasks.increment();
        if (saturation) {
            saturationRejections.increment();
        }
        consecutiveRejections.incrementAndGet();
        lastRejectedAtEpochMillis.set(System.currentTimeMillis());
    }

    public void beginDrain(String reason) {
        acceptingTasks.set(false);
    }

    public boolean acceptingTasks() {
        return acceptingTasks.get() && !shutdown;
    }

    public boolean awaitQuiescence(Duration timeout) {
        long deadline = System.nanoTime() + sanitizeTimeout(timeout, terminationTimeout).toNanos();
        while (System.nanoTime() < deadline) {
            if (activeTasks() <= 0) {
                return true;
            }
            try {
                Thread.sleep(25L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return activeTasks() <= 0;
    }

    public int concurrencyLimit() {
        return concurrencyLimit;
    }

    public int availablePermits() {
        return permits.availablePermits();
    }

    public int activeTasks() {
        return concurrencyLimit - permits.availablePermits();
    }

    public String threadNamePrefix() {
        return threadNamePrefix;
    }

    public long submittedTasks() {
        return submittedTasks.sum();
    }

    public long completedTasks() {
        return completedTasks.sum();
    }

    public long rejectedTasks() {
        return rejectedTasks.sum();
    }

    public long saturationRejections() {
        return saturationRejections.sum();
    }

    public long totalAcquireWaitNanos() {
        return totalAcquireWaitNanos.sum();
    }

    public double averageAcquireWaitMillis() {
        long attempts = acquireAttempts.sum();
        if (attempts <= 0L) {
            return 0.0d;
        }
        return totalAcquireWaitNanos.sum() / 1_000_000.0d / attempts;
    }

    public int maxObservedActiveTasks() {
        return maxObservedActiveTasks.get();
    }

    public int consecutiveRejections() {
        return consecutiveRejections.get();
    }

    public long lastRejectedAtEpochMillis() {
        return lastRejectedAtEpochMillis.get();
    }

    public long lastCompletedAtEpochMillis() {
        return lastCompletedAtEpochMillis.get();
    }

    public double utilizationRatio() {
        return Math.min(1.0d, activeTasks() / (double) concurrencyLimit);
    }

    public long millisSinceLastRejection() {
        long timestamp = lastRejectedAtEpochMillis.get();
        if (timestamp <= 0L) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, System.currentTimeMillis() - timestamp);
    }

    public Instant lastRejectedAt() {
        long timestamp = lastRejectedAtEpochMillis.get();
        return timestamp <= 0L ? null : Instant.ofEpochMilli(timestamp);
    }

    public boolean overloaded(double utilizationThreshold, Duration recentRejectionWindow, int minimumConsecutiveRejections) {
        boolean saturatedByUtilization = utilizationRatio() >= Math.max(0.50d, Math.min(0.999d, utilizationThreshold));
        boolean recentRejection = millisSinceLastRejection() <= Math.max(0L, recentRejectionWindow == null ? 0L : recentRejectionWindow.toMillis());
        boolean rejectionBurst = consecutiveRejections.get() >= Math.max(1, minimumConsecutiveRejections);
        return saturatedByUtilization && recentRejection && rejectionBurst;
    }

    @Override
    public void shutdown() {
        acceptingTasks.set(false);
        shutdown = true;
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        acceptingTasks.set(false);
        shutdown = true;
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return shutdown || delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long requestedNanos = Math.max(0L, unit.toNanos(timeout));
        long configuredNanos = terminationTimeout.toNanos();
        long effective = requestedNanos == 0L ? configuredNanos : Math.min(requestedNanos, configuredNanos);
        return delegate.awaitTermination(effective, TimeUnit.NANOSECONDS);
    }

    @Override
    public void close() {
        shutdown();
        boolean terminated = false;
        try {
            terminated = awaitTermination(terminationTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (!terminated) {
            shutdownNow();
        }
    }

    private void updateMaxObservedActiveTasks() {
        int active = activeTasks();
        maxObservedActiveTasks.accumulateAndGet(active, Math::max);
    }

    private Duration sanitizeTimeout(Duration value, Duration fallback) {
        if (value == null || value.isNegative() || value.isZero()) {
            return fallback;
        }
        return value;
    }
}
