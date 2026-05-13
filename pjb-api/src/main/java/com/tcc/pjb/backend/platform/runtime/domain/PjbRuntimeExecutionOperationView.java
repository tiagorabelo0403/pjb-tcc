package com.tcc.pjb.backend.platform.runtime.domain;

import java.time.Instant;

public record PjbRuntimeExecutionOperationView(String operationName,
                                               String lane,
                                               boolean critical,
                                               int activeTasks,
                                               int maxObservedConcurrency,
                                               long submittedTasks,
                                               long completedTasks,
                                               long failedTasks,
                                               long rejectedTasks,
                                               long timedOutTasks,
                                               double averageLatencyMillis,
                                               Instant lastStartedAt,
                                               Instant lastCompletedAt) {
}
