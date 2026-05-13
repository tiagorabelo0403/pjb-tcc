package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.configs.datasource.PjbDatasourceBudgetProperties;
import com.tcc.pjb.backend.configs.live.NoOpLiveClusterStateStore;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import com.tcc.pjb.backend.platform.runtime.domain.PjbRuntimeReadinessView;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class PjbRuntimePressureHealthIndicatorTest {

    @Test
    void shouldReportOutOfServiceWhenDraining() {
        PjbRuntimePressureProperties pressureProperties = new PjbRuntimePressureProperties();
        pressureProperties.setMinimumReadyAge(Duration.ZERO);
        PjbRuntimeAccelerationProperties accelerationProperties = new PjbRuntimeAccelerationProperties();
        PjbDatasourceBudgetProperties budgetProperties = new PjbDatasourceBudgetProperties();
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 4, true, Duration.ofSeconds(5), Duration.ofMillis(10));
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(8);
        dataSource.setMinimumIdle(2);
        ScheduledExecutorService scheduler = scheduler();
        PjbRuntimePressureService pressureService = new PjbRuntimePressureService(
                pressureProperties,
                accelerationProperties,
                new PjbRuntimeSizingPolicy.Footprint(4, 2048),
                budgetProperties,
                Map.of("pjbIoExecutorService", executor),
                Map.of("pjbWriteDataSource", dataSource),
                scheduler
        );
        PjbRuntimeDrainService drainService = new PjbRuntimeDrainService(new PjbRuntimeLifecycleProperties(), event -> { });
        drainService.markAccepting("ready");
        drainService.beginDrain("shutdown");
        PjbRuntimeReadinessService readinessService = org.mockito.Mockito.mock(PjbRuntimeReadinessService.class);
        org.mockito.Mockito.when(readinessService.snapshot()).thenReturn(new PjbRuntimeReadinessView(
                Instant.now(),
                false,
                "WARMING_UP",
                false,
                pressureService.snapshot(),
                drainService.snapshot(),
                new PjbLivePressureService(pressureProperties, new NoOpLiveClusterStateStore()).snapshot(false),
                new PjbKafkaPressureService(pressureProperties, Map.of()).snapshot(false),
                new com.tcc.pjb.backend.platform.runtime.domain.PjbTransactionPressureView(Instant.now(), 0, 0, 0, 0, 0, 0, 0, 0, false, false, false, List.of())
        ));
        PjbRuntimePressureHealthIndicator indicator = new PjbRuntimePressureHealthIndicator(readinessService);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        executor.close();
        scheduler.shutdownNow();
        dataSource.close();
    }

    @Test
    void shouldReportOutOfServiceWhenStillWarmingUp() {
        PjbRuntimePressureProperties pressureProperties = new PjbRuntimePressureProperties();
        pressureProperties.setMinimumReadyAge(Duration.ofMinutes(1));
        PjbRuntimeAccelerationProperties accelerationProperties = new PjbRuntimeAccelerationProperties();
        PjbDatasourceBudgetProperties budgetProperties = new PjbDatasourceBudgetProperties();
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 4, true, Duration.ofSeconds(5), Duration.ofMillis(10));
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(8);
        dataSource.setMinimumIdle(2);
        ScheduledExecutorService scheduler = scheduler();
        PjbRuntimePressureService pressureService = new PjbRuntimePressureService(
                pressureProperties,
                accelerationProperties,
                new PjbRuntimeSizingPolicy.Footprint(4, 2048),
                budgetProperties,
                Map.of("pjbIoExecutorService", executor),
                Map.of("pjbWriteDataSource", dataSource),
                scheduler
        );
        PjbRuntimeDrainService drainService = new PjbRuntimeDrainService(new PjbRuntimeLifecycleProperties(), event -> { });
        drainService.markAccepting("ready");
        PjbRuntimeReadinessService readinessService = org.mockito.Mockito.mock(PjbRuntimeReadinessService.class);
        org.mockito.Mockito.when(readinessService.snapshot()).thenReturn(new PjbRuntimeReadinessView(
                Instant.now(),
                false,
                "DRAINING",
                false,
                pressureService.snapshot(),
                drainService.snapshot(),
                new PjbLivePressureService(pressureProperties, new NoOpLiveClusterStateStore()).snapshot(false),
                new PjbKafkaPressureService(pressureProperties, Map.of()).snapshot(false),
                new com.tcc.pjb.backend.platform.runtime.domain.PjbTransactionPressureView(Instant.now(), 0, 0, 0, 0, 0, 0, 0, 0, false, false, false, List.of())
        ));
        PjbRuntimePressureHealthIndicator indicator = new PjbRuntimePressureHealthIndicator(readinessService);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        executor.close();
        scheduler.shutdownNow();
        dataSource.close();
    }

    private ScheduledExecutorService scheduler() {
        ThreadFactory threadFactory = Thread.ofPlatform().name("pjb-test-scheduler-", 0).factory();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory);
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }
}
