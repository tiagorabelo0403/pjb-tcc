package com.tcc.pjb.backend.platform.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PjbRuntimeDrainCoordinator implements SmartLifecycle {

    private static final String STOP_REASON = "context-stop";

    private final PjbRuntimeDrainService drainService;
    private final Map<String, PjbBoundedExecutorService> executors;
    private final ScheduledExecutorService timeoutScheduler;
    private final ApplicationContext applicationContext;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicBoolean drainedByStop = new AtomicBoolean();

    public PjbRuntimeDrainCoordinator(PjbRuntimeDrainService drainService,
                                      Map<String, PjbBoundedExecutorService> executors,
                                      @Qualifier("pjbTimeoutScheduler") ScheduledExecutorService timeoutScheduler,
                                      ApplicationContext applicationContext) {
        this.drainService = drainService;
        this.executors = executors;
        this.timeoutScheduler = timeoutScheduler;
        this.applicationContext = applicationContext;
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        if (event.getApplicationContext() == applicationContext) {
            closing.set(true);
        }
    }

    @Override
    public void start() {
        if (drainedByStop.getAndSet(false) && STOP_REASON.equals(drainService.reason())) {
            drainService.markAccepting("context-restart");
        }
        executors.values().forEach(PjbBoundedExecutorService::resumeAccepting);
        stopping.set(false);
        running.set(true);
    }

    @Override
    public void stop() {
        CountDownLatch latch = new CountDownLatch(1);
        stop(latch::countDown);
        try {
            long timeout = Math.max(1000L, drainService.shutdownAwaitTimeout().toMillis());
            latch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop(Runnable callback) {
        if (!running.get()) {
            callback.run();
            return;
        }
        if (!stopping.compareAndSet(false, true)) {
            callback.run();
            return;
        }
        if (!closing.get()) {
            suspend();
            callback.run();
            return;
        }
        drainService.beginDrain("context-shutdown");
        executors.values().forEach(executor -> executor.beginDrain("context-shutdown"));
        Thread.ofPlatform().name("pjb-drain-coordinator").start(() -> {
            try {
                Thread.sleep(Math.max(0L, drainService.drainQuietPeriod().toMillis()));
                timeoutScheduler.shutdown();
                executors.values().forEach(PjbBoundedExecutorService::shutdown);
                awaitQuiescence(drainService.shutdownAwaitTimeout());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                timeoutScheduler.shutdownNow();
                executors.values().forEach(PjbBoundedExecutorService::shutdownNow);
            } finally {
                running.set(false);
                callback.run();
            }
        });
    }

    private void suspend() {
        if (!drainService.isDraining()) {
            drainedByStop.set(drainService.beginDrain(STOP_REASON));
        }
        executors.values().forEach(executor -> executor.beginDrain(STOP_REASON));
        running.set(false);
    }

    private void awaitQuiescence(Duration timeout) throws InterruptedException {
        Duration effective = timeout == null || timeout.isNegative() || timeout.isZero() ? Duration.ofSeconds(30) : timeout;
        long deadline = System.nanoTime() + effective.toNanos();
        for (PjbBoundedExecutorService executor : executors.values()) {
            long remaining = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()));
            executor.awaitQuiescence(Duration.ofMillis(remaining));
            executor.awaitTermination(remaining, TimeUnit.MILLISECONDS);
        }
        long remainingScheduler = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()));
        timeoutScheduler.awaitTermination(remainingScheduler, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
