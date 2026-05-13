package com.tcc.pjb.backend.platform.runtime.execution;

import com.tcc.pjb.backend.configs.datasource.PjbProcessoSigiloRlsContext;
import com.tcc.pjb.backend.platform.runtime.PjbBoundedExecutorProvider;
import com.tcc.pjb.backend.platform.runtime.PjbBoundedExecutorService;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeExecutionGovernanceView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeExecutionLaneView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeExecutionOperationView;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PjbExecutionOrchestrator {

    private final PjbBoundedExecutorProvider boundedExecutorProvider;
    private final ScheduledExecutorService timeoutScheduler;
    private final PjbProcessoSigiloRlsContext processoSigiloRlsContext;
    private final ConcurrentMap<String, OperationTracker> trackers = new ConcurrentHashMap<>();

    public PjbExecutionOrchestrator(PjbBoundedExecutorProvider boundedExecutorProvider,
                                    @Qualifier("pjbTimeoutScheduler") ScheduledExecutorService timeoutScheduler,
                                    PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        this.boundedExecutorProvider = Objects.requireNonNull(boundedExecutorProvider, "boundedExecutorProvider");
        this.timeoutScheduler = Objects.requireNonNull(timeoutScheduler, "timeoutScheduler");
        this.processoSigiloRlsContext = Objects.requireNonNull(processoSigiloRlsContext, "processoSigiloRlsContext");
    }

    public <T> CompletableFuture<T> supply(PjbExecutionDescriptor descriptor, Supplier<T> supplier) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(supplier, "supplier");
        PjbBoundedExecutorService executor = resolveExecutor(descriptor.lane());
        OperationTracker tracker = trackers.computeIfAbsent(descriptor.operationName(), ignored -> new OperationTracker(descriptor.operationName(), descriptor.lane(), descriptor.critical()));
        tracker.markSubmitted();
        CompletableFuture<T> future = new CompletableFuture<>();
        AtomicReference<Thread> runningThread = new AtomicReference<>();
        PjbProcessoSigiloRlsContext.SessionSettings capturedSessionSettings = processoSigiloRlsContext.current();
        ScheduledFuture<?> timeoutTask = scheduleTimeout(descriptor, future, tracker, runningThread);
        try {
            executor.execute(() -> executeTracked(descriptor, supplier, future, tracker, runningThread, capturedSessionSettings));
        } catch (RejectedExecutionException ex) {
            cancelTimeout(timeoutTask);
            tracker.markRejected();
            future.completeExceptionally(new PjbExecutionRejectedException("execution rejected for operation " + descriptor.operationName(), ex));
        } catch (RuntimeException ex) {
            cancelTimeout(timeoutTask);
            tracker.markFailed(0L);
            future.completeExceptionally(ex);
        }
        future.whenComplete((result, error) -> {
            cancelTimeout(timeoutTask);
            if (future.isCancelled()) {
                interrupt(runningThread);
            }
        });
        return future;
    }

    public CompletableFuture<Void> run(PjbExecutionDescriptor descriptor, Runnable task) {
        Objects.requireNonNull(task, "task");
        return supply(descriptor, () -> {
            task.run();
            return null;
        });
    }

    public PjbRuntimeExecutionGovernanceView snapshot() {
        List<PjbRuntimeExecutionLaneView> lanes = List.of(
                toLaneView("io", boundedExecutorProvider.io()),
                toLaneView("burst", boundedExecutorProvider.burst()),
                toLaneView("external-io", boundedExecutorProvider.externalIo()),
                toLaneView("live", boundedExecutorProvider.live()),
                toLaneView("job", boundedExecutorProvider.job())
        );
        List<PjbRuntimeExecutionOperationView> operations = trackers.values().stream()
                .map(OperationTracker::toView)
                .sorted(Comparator.comparing(PjbRuntimeExecutionOperationView::critical).reversed()
                        .thenComparing(PjbRuntimeExecutionOperationView::activeTasks, Comparator.reverseOrder())
                        .thenComparing(PjbRuntimeExecutionOperationView::submittedTasks, Comparator.reverseOrder())
                        .thenComparing(PjbRuntimeExecutionOperationView::operationName))
                .toList();
        return new PjbRuntimeExecutionGovernanceView(Instant.now(), lanes, operations);
    }

    private ScheduledFuture<?> scheduleTimeout(PjbExecutionDescriptor descriptor,
                                               CompletableFuture<?> future,
                                               OperationTracker tracker,
                                               AtomicReference<Thread> runningThread) {
        long timeoutMillis = Math.max(1L, descriptor.timeout().toMillis());
        return timeoutScheduler.schedule(() -> {
            if (future.completeExceptionally(new PjbExecutionTimedOutException("execution timed out for operation " + descriptor.operationName()))) {
                tracker.markTimedOut();
                interrupt(runningThread);
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private <T> void executeTracked(PjbExecutionDescriptor descriptor,
                                    Supplier<T> supplier,
                                    CompletableFuture<T> future,
                                    OperationTracker tracker,
                                    AtomicReference<Thread> runningThread,
                                    PjbProcessoSigiloRlsContext.SessionSettings capturedSessionSettings) {
        if (future.isDone()) {
            return;
        }
        long startedAt = System.nanoTime();
        Thread currentThread = Thread.currentThread();
        runningThread.set(currentThread);
        tracker.markStarted();
        PjbProcessoSigiloRlsContext.SessionSettings previousSessionSettings = processoSigiloRlsContext.current();
        try {
            processoSigiloRlsContext.restore(capturedSessionSettings);
            if (future.isDone()) {
                currentThread.interrupt();
                return;
            }
            T result = supplier.get();
            if (future.complete(result)) {
                tracker.markCompleted(System.nanoTime() - startedAt);
            }
        } catch (Throwable ex) {
            if (future.completeExceptionally(ex)) {
                tracker.markFailed(System.nanoTime() - startedAt);
            }
        } finally {
            processoSigiloRlsContext.restore(previousSessionSettings);
            runningThread.compareAndSet(currentThread, null);
            tracker.markFinished();
        }
    }

    private void cancelTimeout(ScheduledFuture<?> timeoutTask) {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
        }
    }

    private void interrupt(AtomicReference<Thread> runningThread) {
        if (runningThread == null) {
            return;
        }
        Thread thread = runningThread.get();
        if (thread != null) {
            thread.interrupt();
        }
    }

    private PjbBoundedExecutorService resolveExecutor(PjbExecutionLane lane) {
        return switch (lane) {
            case IO -> boundedExecutorProvider.io();
            case BURST -> boundedExecutorProvider.burst();
            case EXTERNAL_IO -> boundedExecutorProvider.externalIo();
            case LIVE -> boundedExecutorProvider.live();
            case JOB -> boundedExecutorProvider.job();
        };
    }

    private PjbRuntimeExecutionLaneView toLaneView(String lane, PjbBoundedExecutorService executor) {
        return new PjbRuntimeExecutionLaneView(
                lane,
                executor.threadNamePrefix(),
                executor.concurrencyLimit(),
                executor.activeTasks(),
                executor.availablePermits(),
                executor.utilizationRatio(),
                executor.submittedTasks(),
                executor.completedTasks(),
                executor.rejectedTasks(),
                executor.saturationRejections(),
                executor.averageAcquireWaitMillis(),
                executor.acceptingTasks()
        );
    }

    private static final class OperationTracker {

        private final String operationName;
        private final PjbExecutionLane lane;
        private final boolean critical;
        private final LongAdder submittedTasks = new LongAdder();
        private final LongAdder completedTasks = new LongAdder();
        private final LongAdder failedTasks = new LongAdder();
        private final LongAdder rejectedTasks = new LongAdder();
        private final LongAdder timedOutTasks = new LongAdder();
        private final LongAdder totalLatencyNanos = new LongAdder();
        private final AtomicInteger activeTasks = new AtomicInteger();
        private final AtomicInteger maxObservedConcurrency = new AtomicInteger();
        private final AtomicLong lastStartedAtEpochMillis = new AtomicLong();
        private final AtomicLong lastCompletedAtEpochMillis = new AtomicLong();

        private OperationTracker(String operationName, PjbExecutionLane lane, boolean critical) {
            this.operationName = operationName;
            this.lane = lane;
            this.critical = critical;
        }

        private void markSubmitted() {
            submittedTasks.increment();
        }

        private void markStarted() {
            int current = activeTasks.incrementAndGet();
            maxObservedConcurrency.accumulateAndGet(current, Math::max);
            lastStartedAtEpochMillis.set(System.currentTimeMillis());
        }

        private void markCompleted(long latencyNanos) {
            completedTasks.increment();
            totalLatencyNanos.add(Math.max(0L, latencyNanos));
            lastCompletedAtEpochMillis.set(System.currentTimeMillis());
        }

        private void markFailed(long latencyNanos) {
            failedTasks.increment();
            if (latencyNanos > 0L) {
                totalLatencyNanos.add(latencyNanos);
            }
            lastCompletedAtEpochMillis.set(System.currentTimeMillis());
        }

        private void markRejected() {
            rejectedTasks.increment();
            lastCompletedAtEpochMillis.set(System.currentTimeMillis());
        }

        private void markTimedOut() {
            timedOutTasks.increment();
            lastCompletedAtEpochMillis.set(System.currentTimeMillis());
        }

        private void markFinished() {
            activeTasks.updateAndGet(value -> Math.max(0, value - 1));
        }

        private PjbRuntimeExecutionOperationView toView() {
            long completedLike = completedTasks.sum() + failedTasks.sum();
            double averageLatencyMillis = completedLike <= 0L
                    ? 0.0d
                    : totalLatencyNanos.sum() / 1_000_000.0d / completedLike;
            return new PjbRuntimeExecutionOperationView(
                    operationName,
                    lane.name(),
                    critical,
                    activeTasks.get(),
                    maxObservedConcurrency.get(),
                    submittedTasks.sum(),
                    completedTasks.sum(),
                    failedTasks.sum(),
                    rejectedTasks.sum(),
                    timedOutTasks.sum(),
                    averageLatencyMillis,
                    instantOf(lastStartedAtEpochMillis.get()),
                    instantOf(lastCompletedAtEpochMillis.get())
            );
        }

        private Instant instantOf(long epochMillis) {
            return epochMillis <= 0L ? null : Instant.ofEpochMilli(epochMillis);
        }
    }
}
