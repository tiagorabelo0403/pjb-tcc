package com.tcc.pjb.backend.platform.runtime;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PjbTransactionalPressureTrackerTest {

    @Test
    void deveRegistrarTransacaoLongaNoSnapshot() throws Exception {
        PjbRuntimePressureProperties properties = new PjbRuntimePressureProperties();
        properties.setTransactionLongRunningThreshold(Duration.ofMillis(20));
        properties.setTransactionActiveThreshold(1);
        PjbTransactionalPressureTracker tracker = new PjbTransactionalPressureTracker(properties);

        PjbTransactionalPressureTracker.Handle handle = tracker.start("AcordoSuggestionPipelineAsyncService.runForProposalInternal", false, "REQUIRES_NEW");
        Thread.sleep(30L);
        tracker.complete(handle, null);

        var snapshot = tracker.snapshot();
        assertEquals(1L, snapshot.longRunningTransactions());
        assertTrue(snapshot.longRunningPressure());
        assertTrue(snapshot.operations().stream().anyMatch(operation -> operation.operationName().contains("AcordoSuggestionPipelineAsyncService") && operation.longRunningTransactions() == 1L));
    }

    @Test
    void deveSinalizarViolacaoDeBudgetNoSnapshot() throws Exception {
        PjbRuntimePressureProperties properties = new PjbRuntimePressureProperties();
        properties.setTransactionLongRunningThreshold(Duration.ofSeconds(5));
        properties.setTransactionBudgetViolationThreshold(1);
        properties.setCriticalTransactionBudgetViolationThreshold(1);
        PjbTransactionalPressureTracker tracker = new PjbTransactionalPressureTracker(properties);

        PjbTransactionalPressureTracker.Handle handle = tracker.start(
                "sobrestamento.tema.persist-batch",
                false,
                "REQUIRED",
                Duration.ofMillis(15),
                true
        );
        Thread.sleep(25L);
        tracker.complete(handle, null);

        var snapshot = tracker.snapshot();
        assertTrue(snapshot.budgetPressure());
        assertEquals(1L, snapshot.budgetViolations());
        assertEquals(1L, snapshot.criticalBudgetViolations());
        assertTrue(snapshot.operations().stream().anyMatch(operation -> operation.operationName().equals("sobrestamento.tema.persist-batch") && operation.budgetViolations() == 1L && operation.criticalBudget()));
    }
}
