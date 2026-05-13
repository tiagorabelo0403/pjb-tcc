package com.tcc.pjb.backend.platform.runtime;

import com.tcc.pjb.backend.platform.runtime.domain.PjbTransactionOperationView;
import com.tcc.pjb.backend.platform.runtime.domain.PjbTransactionPressureView;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

@Component
public class PjbTransactionalPressureTracker {

    private final PjbRuntimePressureProperties properties;
    private final ConcurrentMap<String, OperationTracker> trackers = new ConcurrentHashMap<>();
    private final AtomicInteger activeTransactions = new AtomicInteger();
    private final AtomicInteger maxObservedConcurrency = new AtomicInteger();
    private final LongAdder startedTransactions = new LongAdder();
    private final LongAdder completedTransactions = new LongAdder();
    private final LongAdder failedTransactions = new LongAdder();
    private final LongAdder longRunningTransactions = new LongAdder();
    private final LongAdder budgetViolations = new LongAdder();
    private final LongAdder criticalBudgetViolations = new LongAdder();

    public PjbTransactionalPressureTracker(PjbRuntimePressureProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public Handle start(String operationName, boolean readOnly, String propagation) {
        return start(operationName, readOnly, propagation, properties.getTransactionLongRunningThreshold(), false);
    }

    public Handle start(String operationName,
                        boolean readOnly,
                        String propagation,
                        Duration budget,
                        boolean criticalBudget) {
        String normalizedOperationName = sanitize(operationName);
        Duration normalizedBudget = normalizeBudget(budget);
        OperationTracker tracker = trackers.computeIfAbsent(
                normalizedOperationName,
                ignored -> new OperationTracker(normalizedOperationName, readOnly, propagation)
        );
        tracker.applyBudget(normalizedBudget, criticalBudget);
        startedTransactions.increment();
        tracker.markStarted();
        int current = activeTransactions.incrementAndGet();
        maxObservedConcurrency.accumulateAndGet(current, Math::max);
        return new Handle(tracker, System.nanoTime(), normalizedBudget.toNanos(), criticalBudget);
    }

    public void complete(Handle handle, Throwable error) {
        if (handle == null) {
            return;
        }
        long elapsedNanos = Math.max(0L, System.nanoTime() - handle.startedAtNanos());
        Duration longRunningThreshold = properties.getTransactionLongRunningThreshold();
        boolean longRunning = elapsedNanos >= Math.max(1L, longRunningThreshold.toNanos());
        boolean budgetExceeded = handle.budgetNanos() > 0L && elapsedNanos > handle.budgetNanos();
        if (error == null) {
            completedTransactions.increment();
        } else {
            failedTransactions.increment();
        }
        if (longRunning) {
            longRunningTransactions.increment();
        }
        if (budgetExceeded) {
            budgetViolations.increment();
            if (handle.criticalBudget()) {
                criticalBudgetViolations.increment();
            }
        }
        handle.tracker().markFinished(elapsedNanos, error == null, longRunning, budgetExceeded, handle.budgetNanos(), handle.criticalBudget());
        activeTransactions.updateAndGet(value -> Math.max(0, value - 1));
    }

    public PjbTransactionPressureView snapshot() {
        int active = activeTransactions.get();
        long longRunning = longRunningTransactions.sum();
        long budgetViolationCount = budgetViolations.sum();
        long criticalBudgetViolationCount = criticalBudgetViolations.sum();
        boolean activePressure = active >= properties.getTransactionActiveThreshold();
        boolean longRunningPressure = longRunning > 0L;
        boolean budgetPressure = budgetViolationCount >= properties.getTransactionBudgetViolationThreshold()
                || criticalBudgetViolationCount >= properties.getCriticalTransactionBudgetViolationThreshold();
        List<PjbTransactionOperationView> operations = trackers.values().stream()
                .map(OperationTracker::toView)
                .sorted(Comparator.comparing(PjbTransactionOperationView::budgetViolations, Comparator.reverseOrder())
                        .thenComparing(PjbTransactionOperationView::longRunningTransactions, Comparator.reverseOrder())
                        .thenComparing(PjbTransactionOperationView::activeTransactions, Comparator.reverseOrder())
                        .thenComparing(PjbTransactionOperationView::averageDurationMillis, Comparator.reverseOrder())
                        .thenComparing(PjbTransactionOperationView::operationName))
                .limit(40)
                .toList();
        return new PjbTransactionPressureView(
                Instant.now(),
                active,
                maxObservedConcurrency.get(),
                startedTransactions.sum(),
                completedTransactions.sum(),
                failedTransactions.sum(),
                longRunning,
                budgetViolationCount,
                criticalBudgetViolationCount,
                longRunningPressure,
                activePressure,
                budgetPressure,
                operations
        );
    }

    private String sanitize(String operationName) {
        if (operationName == null || operationName.isBlank()) {
            return "transactional-operation";
        }
        return operationName.trim();
    }

    private Duration normalizeBudget(Duration budget) {
        if (budget == null || budget.isZero() || budget.isNegative()) {
            return properties.getTransactionLongRunningThreshold();
        }
        return budget;
    }

    public record Handle(OperationTracker tracker, long startedAtNanos, long budgetNanos, boolean criticalBudget) {
    }

    static final class OperationTracker {

        private final String operationName;
        private final boolean readOnly;
        private final String propagation;
        private final AtomicInteger activeTransactions = new AtomicInteger();
        private final AtomicInteger maxObservedConcurrency = new AtomicInteger();
        private final LongAdder startedTransactions = new LongAdder();
        private final LongAdder completedTransactions = new LongAdder();
        private final LongAdder failedTransactions = new LongAdder();
        private final LongAdder longRunningTransactions = new LongAdder();
        private final LongAdder budgetViolations = new LongAdder();
        private final LongAdder totalDurationNanos = new LongAdder();
        private final DoubleAccumulator maxDurationMillis = new DoubleAccumulator(Double::max, 0.0d);
        private final LongAccumulator budgetNanos = new LongAccumulator(Long::min, Long.MAX_VALUE);
        private final AtomicBoolean budgetConfigured = new AtomicBoolean();
        private final AtomicBoolean criticalBudget = new AtomicBoolean();

        private OperationTracker(String operationName, boolean readOnly, String propagation) {
            this.operationName = operationName;
            this.readOnly = readOnly;
            this.propagation = propagation == null || propagation.isBlank() ? "REQUIRED" : propagation;
        }

        private void applyBudget(Duration budget, boolean criticalBudget) {
            if (budget != null && !budget.isNegative() && !budget.isZero()) {
                budgetConfigured.set(true);
                budgetNanos.accumulate(budget.toNanos());
            }
            if (criticalBudget) {
                this.criticalBudget.set(true);
            }
        }

        private void markStarted() {
            startedTransactions.increment();
            int current = activeTransactions.incrementAndGet();
            maxObservedConcurrency.accumulateAndGet(current, Math::max);
        }

        private void markFinished(long elapsedNanos,
                                  boolean success,
                                  boolean longRunning,
                                  boolean budgetExceeded,
                                  long resolvedBudgetNanos,
                                  boolean criticalBudget) {
            if (success) {
                completedTransactions.increment();
            } else {
                failedTransactions.increment();
            }
            if (longRunning) {
                longRunningTransactions.increment();
            }
            if (budgetExceeded) {
                budgetViolations.increment();
            }
            if (resolvedBudgetNanos > 0L) {
                budgetConfigured.set(true);
                budgetNanos.accumulate(resolvedBudgetNanos);
            }
            if (criticalBudget) {
                this.criticalBudget.set(true);
            }
            totalDurationNanos.add(elapsedNanos);
            maxDurationMillis.accumulate(elapsedNanos / 1_000_000.0d);
            activeTransactions.updateAndGet(value -> Math.max(0, value - 1));
        }

        private PjbTransactionOperationView toView() {
            long finished = completedTransactions.sum() + failedTransactions.sum();
            double averageDurationMillis = finished <= 0L ? 0.0d : totalDurationNanos.sum() / 1_000_000.0d / finished;
            long resolvedBudgetNanos = budgetConfigured.get() ? budgetNanos.get() : Long.MAX_VALUE;
            boolean configured = budgetConfigured.get() && resolvedBudgetNanos != Long.MAX_VALUE;
            return new PjbTransactionOperationView(
                    operationName,
                    readOnly,
                    propagation,
                    activeTransactions.get(),
                    maxObservedConcurrency.get(),
                    startedTransactions.sum(),
                    completedTransactions.sum(),
                    failedTransactions.sum(),
                    longRunningTransactions.sum(),
                    budgetViolations.sum(),
                    configured,
                    configured ? resolvedBudgetNanos / 1_000_000.0d : 0.0d,
                    criticalBudget.get(),
                    budgetViolations.sum() > 0L,
                    averageDurationMillis,
                    maxDurationMillis.get()
            );
        }
    }
}
