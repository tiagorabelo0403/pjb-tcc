package com.tcc.pjb.backend.platform.runtime.domain;

public record PjbTransactionOperationView(String operationName,
                                          boolean readOnly,
                                          String propagation,
                                          int activeTransactions,
                                          int maxObservedConcurrency,
                                          long startedTransactions,
                                          long completedTransactions,
                                          long failedTransactions,
                                          long longRunningTransactions,
                                          long budgetViolations,
                                          boolean budgetConfigured,
                                          double budgetMillis,
                                          boolean criticalBudget,
                                          boolean budgetPressure,
                                          double averageDurationMillis,
                                          double maxDurationMillis) {
}
