package com.tcc.pjb.backend.platform.runtime.domain;

import java.time.Instant;
import java.util.List;

public record PjbTransactionPressureView(Instant generatedAt,
                                         int activeTransactions,
                                         int maxObservedConcurrency,
                                         long startedTransactions,
                                         long completedTransactions,
                                         long failedTransactions,
                                         long longRunningTransactions,
                                         long budgetViolations,
                                         long criticalBudgetViolations,
                                         boolean longRunningPressure,
                                         boolean activePressure,
                                         boolean budgetPressure,
                                         List<PjbTransactionOperationView> operations) {
}
