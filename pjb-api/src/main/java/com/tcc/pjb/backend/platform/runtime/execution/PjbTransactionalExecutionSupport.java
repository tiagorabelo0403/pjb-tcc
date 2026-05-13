package com.tcc.pjb.backend.platform.runtime.execution;

import com.tcc.pjb.backend.platform.runtime.PjbRuntimePressureProperties;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalPressureTracker;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PjbTransactionalExecutionSupport {

    private final PjbExecutionOrchestrator executionOrchestrator;
    private final PlatformTransactionManager transactionManager;
    private final PjbTransactionalPressureTracker transactionalPressureTracker;
    private final PjbRuntimePressureProperties pressureProperties;

    public PjbTransactionalExecutionSupport(PjbExecutionOrchestrator executionOrchestrator,
                                            PlatformTransactionManager transactionManager,
                                            PjbTransactionalPressureTracker transactionalPressureTracker,
                                            PjbRuntimePressureProperties pressureProperties) {
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
        this.transactionManager = Objects.requireNonNull(transactionManager, "transactionManager");
        this.transactionalPressureTracker = Objects.requireNonNull(transactionalPressureTracker, "transactionalPressureTracker");
        this.pressureProperties = Objects.requireNonNull(pressureProperties, "pressureProperties");
    }

    public CompletableFuture<Void> run(PjbExecutionDescriptor descriptor, Runnable task) {
        Objects.requireNonNull(task, "task");
        return executionOrchestrator.run(descriptor, task);
    }

    public <T> CompletableFuture<T> supply(PjbExecutionDescriptor descriptor, Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return executionOrchestrator.supply(descriptor, supplier);
    }

    public CompletableFuture<Void> runInTransaction(PjbExecutionDescriptor descriptor, Runnable task) {
        return runInTransaction(descriptor, defaultTransactionBudget(), task);
    }

    public CompletableFuture<Void> runInTransaction(PjbExecutionDescriptor descriptor, Duration transactionBudget, Runnable task) {
        return run(descriptor, () -> executeInTransaction(descriptor.operationName(), transactionBudget, task));
    }

    public CompletableFuture<Void> runInNewTransaction(PjbExecutionDescriptor descriptor, Runnable task) {
        return runInNewTransaction(descriptor, defaultTransactionBudget(), task);
    }

    public CompletableFuture<Void> runInNewTransaction(PjbExecutionDescriptor descriptor, Duration transactionBudget, Runnable task) {
        return run(descriptor, () -> executeInNewTransaction(descriptor.operationName(), transactionBudget, task));
    }

    public CompletableFuture<Void> runReadOnly(PjbExecutionDescriptor descriptor, Runnable task) {
        return runReadOnly(descriptor, defaultTransactionBudget(), task);
    }

    public CompletableFuture<Void> runReadOnly(PjbExecutionDescriptor descriptor, Duration transactionBudget, Runnable task) {
        return run(descriptor, () -> executeReadOnly(descriptor.operationName(), transactionBudget, task));
    }

    public <T> CompletableFuture<T> supplyInTransaction(PjbExecutionDescriptor descriptor, Supplier<T> supplier) {
        return supplyInTransaction(descriptor, defaultTransactionBudget(), supplier);
    }

    public <T> CompletableFuture<T> supplyInTransaction(PjbExecutionDescriptor descriptor, Duration transactionBudget, Supplier<T> supplier) {
        return supply(descriptor, () -> executeInTransaction(descriptor.operationName(), transactionBudget, supplier));
    }

    public <T> CompletableFuture<T> supplyInNewTransaction(PjbExecutionDescriptor descriptor, Supplier<T> supplier) {
        return supplyInNewTransaction(descriptor, defaultTransactionBudget(), supplier);
    }

    public <T> CompletableFuture<T> supplyInNewTransaction(PjbExecutionDescriptor descriptor, Duration transactionBudget, Supplier<T> supplier) {
        return supply(descriptor, () -> executeInNewTransaction(descriptor.operationName(), transactionBudget, supplier));
    }

    public <T> CompletableFuture<T> supplyReadOnly(PjbExecutionDescriptor descriptor, Supplier<T> supplier) {
        return supplyReadOnly(descriptor, defaultTransactionBudget(), supplier);
    }

    public <T> CompletableFuture<T> supplyReadOnly(PjbExecutionDescriptor descriptor, Duration transactionBudget, Supplier<T> supplier) {
        return supply(descriptor, () -> executeReadOnly(descriptor.operationName(), transactionBudget, supplier));
    }

    public void executeInTransaction(Runnable task) {
        executeInTransaction("transactional-execution", defaultTransactionBudget(), task);
    }

    public void executeInTransaction(String operationName, Duration transactionBudget, Runnable task) {
        executeInTransaction(operationName, transactionBudget, () -> {
            task.run();
            return null;
        });
    }

    public void executeInNewTransaction(Runnable task) {
        executeInNewTransaction("transactional-execution", defaultTransactionBudget(), task);
    }

    public void executeInNewTransaction(String operationName, Duration transactionBudget, Runnable task) {
        executeInNewTransaction(operationName, transactionBudget, () -> {
            task.run();
            return null;
        });
    }

    public void executeReadOnly(Runnable task) {
        executeReadOnly("transactional-execution", defaultTransactionBudget(), task);
    }

    public void executeReadOnly(String operationName, Duration transactionBudget, Runnable task) {
        executeReadOnly(operationName, transactionBudget, () -> {
            task.run();
            return null;
        });
    }

    public <T> T executeInTransaction(Supplier<T> supplier) {
        return executeInTransaction("transactional-execution", defaultTransactionBudget(), supplier);
    }

    public <T> T executeInTransaction(String operationName, Duration transactionBudget, Supplier<T> supplier) {
        return execute(operationName, TransactionDefinition.PROPAGATION_REQUIRED, false, transactionBudget, supplier);
    }

    public <T> T executeInNewTransaction(Supplier<T> supplier) {
        return executeInNewTransaction("transactional-execution", defaultTransactionBudget(), supplier);
    }

    public <T> T executeInNewTransaction(String operationName, Duration transactionBudget, Supplier<T> supplier) {
        return execute(operationName, TransactionDefinition.PROPAGATION_REQUIRES_NEW, false, transactionBudget, supplier);
    }

    public <T> T executeReadOnly(Supplier<T> supplier) {
        return executeReadOnly("transactional-execution", defaultTransactionBudget(), supplier);
    }

    public <T> T executeReadOnly(String operationName, Duration transactionBudget, Supplier<T> supplier) {
        return execute(operationName, TransactionDefinition.PROPAGATION_REQUIRED, true, transactionBudget, supplier);
    }

    private <T> T execute(String operationName,
                          int propagationBehavior,
                          boolean readOnly,
                          Duration transactionBudget,
                          Supplier<T> supplier) {
        Duration normalizedBudget = normalizeBudget(transactionBudget);
        TransactionTemplate template = template(propagationBehavior, readOnly, normalizedBudget);
        PjbTransactionalPressureTracker.Handle handle = transactionalPressureTracker.start(
                normalizeOperationName(operationName),
                readOnly,
                propagationName(propagationBehavior),
                normalizedBudget,
                false
        );
        Throwable failure = null;
        try {
            return template.execute(status -> supplier.get());
        } catch (RuntimeException | Error ex) {
            failure = ex;
            throw ex;
        } finally {
            transactionalPressureTracker.complete(handle, failure);
        }
    }

    private TransactionTemplate template(int propagationBehavior, boolean readOnly, Duration transactionBudget) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(propagationBehavior);
        template.setReadOnly(readOnly);
        Duration normalizedBudget = normalizeBudget(transactionBudget);
        long timeoutSeconds = Math.max(1L, (long) Math.ceil(normalizedBudget.toMillis() / 1000.0d));
        template.setTimeout((int) Math.min(Integer.MAX_VALUE, timeoutSeconds));
        return template;
    }

    private Duration defaultTransactionBudget() {
        return pressureProperties.getTransactionLongRunningThreshold();
    }

    private Duration normalizeBudget(Duration transactionBudget) {
        if (transactionBudget == null || transactionBudget.isNegative() || transactionBudget.isZero()) {
            return defaultTransactionBudget();
        }
        return transactionBudget;
    }

    private String normalizeOperationName(String operationName) {
        if (operationName == null || operationName.isBlank()) {
            return "transactional-execution";
        }
        return operationName.trim();
    }

    private String propagationName(int propagationBehavior) {
        return switch (propagationBehavior) {
            case TransactionDefinition.PROPAGATION_REQUIRES_NEW -> "REQUIRES_NEW";
            case TransactionDefinition.PROPAGATION_SUPPORTS -> "SUPPORTS";
            case TransactionDefinition.PROPAGATION_MANDATORY -> "MANDATORY";
            case TransactionDefinition.PROPAGATION_NOT_SUPPORTED -> "NOT_SUPPORTED";
            case TransactionDefinition.PROPAGATION_NEVER -> "NEVER";
            case TransactionDefinition.PROPAGATION_NESTED -> "NESTED";
            default -> "REQUIRED";
        };
    }
}
