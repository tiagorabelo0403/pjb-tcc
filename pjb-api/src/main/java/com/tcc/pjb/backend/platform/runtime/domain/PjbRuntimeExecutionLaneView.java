package com.tcc.pjb.backend.platform.runtime.domain;

public record PjbRuntimeExecutionLaneView(String lane,
                                          String beanName,
                                          int concurrencyLimit,
                                          int activeTasks,
                                          int availablePermits,
                                          double utilizationRatio,
                                          long submittedTasks,
                                          long completedTasks,
                                          long rejectedTasks,
                                          long saturationRejections,
                                          double averageAcquireWaitMillis,
                                          boolean acceptingTasks) {
}
