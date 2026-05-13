package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.tcc.pjb.backend.configs.datasource.PjbDatasourceBudgetProperties;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PjbRuntimePressureServiceTest {

    @Test
    void shouldReportReadyWhenExecutorsAndPoolsAreCalm() {
        PjbRuntimePressureProperties pressureProperties = new PjbRuntimePressureProperties();
        pressureProperties.setSnapshotCacheTtl(Duration.ZERO);
        pressureProperties.setMinimumReadyAge(Duration.ZERO);
        isolateHostPressure(pressureProperties);
        PjbRuntimeAccelerationProperties accelerationProperties = new PjbRuntimeAccelerationProperties();
        PjbDatasourceBudgetProperties budgetProperties = new PjbDatasourceBudgetProperties();
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 8, true, Duration.ofSeconds(5), Duration.ofMillis(10));
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(12);
        dataSource.setMinimumIdle(2);
        ScheduledExecutorService scheduler = scheduler();
        PjbRuntimePressureService service = new PjbRuntimePressureService(
                pressureProperties,
                accelerationProperties,
                new PjbRuntimeSizingPolicy.Footprint(4, 2048),
                budgetProperties,
                Map.of("pjbIoExecutorService", executor),
                Map.of("pjbWriteDataSource", dataSource),
                scheduler
        );
        PjbRuntimePressureService.Snapshot snapshot = service.snapshot();
        assertThat(snapshot.ready()).isTrue();
        assertThat(snapshot.warmingUp()).isFalse();
        assertThat(snapshot.scheduler().degraded()).isFalse();
        executor.close();
        scheduler.shutdownNow();
        dataSource.close();
    }

    @Test
    void shouldReportWarmingUpBeforeMinimumReadyAgeElapses() {
        PjbRuntimePressureProperties pressureProperties = new PjbRuntimePressureProperties();
        pressureProperties.setSnapshotCacheTtl(Duration.ZERO);
        pressureProperties.setMinimumReadyAge(Duration.ofMinutes(1));
        isolateHostPressure(pressureProperties);
        PjbRuntimeAccelerationProperties accelerationProperties = new PjbRuntimeAccelerationProperties();
        PjbDatasourceBudgetProperties budgetProperties = new PjbDatasourceBudgetProperties();
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 8, true, Duration.ofSeconds(5), Duration.ofMillis(10));
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(12);
        dataSource.setMinimumIdle(2);
        ScheduledExecutorService scheduler = scheduler();
        PjbRuntimePressureService service = new PjbRuntimePressureService(
                pressureProperties,
                accelerationProperties,
                new PjbRuntimeSizingPolicy.Footprint(4, 2048),
                budgetProperties,
                Map.of("pjbIoExecutorService", executor),
                Map.of("pjbWriteDataSource", dataSource),
                scheduler
        );
        PjbRuntimePressureService.Snapshot snapshot = service.snapshot();
        assertThat(snapshot.warmingUp()).isTrue();
        assertThat(snapshot.ready()).isFalse();
        assertThat(snapshot.alerts()).anyMatch(alert -> alert.code().equals("warming_up"));
        executor.close();
        scheduler.shutdownNow();
        dataSource.close();
    }

    @Test
    void shouldFailReadinessWhenCriticalLaneOverloads() throws Exception {
        PjbRuntimePressureProperties pressureProperties = new PjbRuntimePressureProperties();
        pressureProperties.setSnapshotCacheTtl(Duration.ZERO);
        pressureProperties.setMinimumReadyAge(Duration.ZERO);
        isolateHostPressure(pressureProperties);
        pressureProperties.setPressureRisingFastDelta(100);
        PjbRuntimeAccelerationProperties accelerationProperties = new PjbRuntimeAccelerationProperties();
        accelerationProperties.setComponentRole("api");
        PjbDatasourceBudgetProperties budgetProperties = new PjbDatasourceBudgetProperties();
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-ext-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(5));
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(12);
        dataSource.setMinimumIdle(2);
        ScheduledExecutorService scheduler = scheduler();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        executor.execute(() -> {
            entered.countDown();
            await(release);
        });
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        Throwable thrown = catchThrowable(() -> executor.execute(() -> { }));
        PjbRuntimePressureService service = new PjbRuntimePressureService(
                pressureProperties,
                accelerationProperties,
                new PjbRuntimeSizingPolicy.Footprint(4, 2048),
                budgetProperties,
                Map.of("pjbExternalIoExecutorService", executor),
                Map.of("pjbWriteDataSource", dataSource),
                scheduler
        );
        PjbRuntimePressureService.Snapshot snapshot = service.snapshot();
        assertThat(thrown).isNotNull();
        assertThat(snapshot.ready()).isFalse();
        assertThat(snapshot.criticalOverloadedExecutorNames()).contains("pjbExternalIoExecutorService");
        assertThat(snapshot.alerts()).anyMatch(alert -> alert.code().equals("critical_lane_overloaded"));
        release.countDown();
        executor.close();
        scheduler.shutdownNow();
        dataSource.close();
    }

    @Test
    void shouldDetectFastRisingPressure() throws Exception {
        PjbRuntimePressureProperties pressureProperties = new PjbRuntimePressureProperties();
        pressureProperties.setSnapshotCacheTtl(Duration.ZERO);
        pressureProperties.setMinimumReadyAge(Duration.ZERO);
        isolateHostPressure(pressureProperties);
        pressureProperties.setPressureTrendWindow(Duration.ofSeconds(30));
        pressureProperties.setPressureRisingFastDelta(15);
        PjbRuntimeAccelerationProperties accelerationProperties = new PjbRuntimeAccelerationProperties();
        accelerationProperties.setComponentRole("worker");
        PjbDatasourceBudgetProperties budgetProperties = new PjbDatasourceBudgetProperties();
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-job-", 1, true, Duration.ofSeconds(5), Duration.ofMillis(5));
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(12);
        dataSource.setMinimumIdle(2);
        ScheduledExecutorService scheduler = scheduler();
        PjbRuntimePressureService service = new PjbRuntimePressureService(
                pressureProperties,
                accelerationProperties,
                new PjbRuntimeSizingPolicy.Footprint(4, 2048),
                budgetProperties,
                Map.of("pjbJobExecutorService", executor),
                Map.of("pjbWriteDataSource", dataSource),
                scheduler
        );
        PjbRuntimePressureService.Snapshot calm = service.snapshot();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        executor.execute(() -> {
            entered.countDown();
            await(release);
        });
        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        catchThrowable(() -> executor.execute(() -> { }));
        PjbRuntimePressureService.Snapshot stressed = service.snapshot();
        assertThat(calm.pressureScore()).isLessThan(stressed.pressureScore());
        assertThat(stressed.trend().risingFast()).isTrue();
        assertThat(stressed.alerts()).anyMatch(alert -> alert.code().equals("pressure_rising_fast"));
        release.countDown();
        executor.close();
        scheduler.shutdownNow();
        dataSource.close();
    }

    @Test
    void shouldDetectSchedulerQueueTrend() {
        PjbRuntimePressureProperties pressureProperties = new PjbRuntimePressureProperties();
        pressureProperties.setSnapshotCacheTtl(Duration.ZERO);
        pressureProperties.setMinimumReadyAge(Duration.ZERO);
        isolateHostPressure(pressureProperties);
        pressureProperties.setPressureRisingFastDelta(100);
        pressureProperties.setSchedulerQueueRisingFastDelta(1);
        pressureProperties.setSchedulerQueueSizeThreshold(1);
        pressureProperties.setSchedulerTrendWindow(Duration.ofSeconds(30));
        PjbRuntimeAccelerationProperties accelerationProperties = new PjbRuntimeAccelerationProperties();
        PjbDatasourceBudgetProperties budgetProperties = new PjbDatasourceBudgetProperties();
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 8, true, Duration.ofSeconds(5), Duration.ofMillis(10));
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(12);
        dataSource.setMinimumIdle(2);
        ScheduledThreadPoolExecutor scheduler = scheduler();
        PjbRuntimePressureService service = new PjbRuntimePressureService(
                pressureProperties,
                accelerationProperties,
                new PjbRuntimeSizingPolicy.Footprint(4, 2048),
                budgetProperties,
                Map.of("pjbIoExecutorService", executor),
                Map.of("pjbWriteDataSource", dataSource),
                scheduler
        );
        PjbRuntimePressureService.Snapshot calm = service.snapshot();
        scheduler.schedule(() -> { }, 60, TimeUnit.SECONDS);
        scheduler.schedule(() -> { }, 60, TimeUnit.SECONDS);
        PjbRuntimePressureService.Snapshot stressed = service.snapshot();
        assertThat(calm.scheduler().risingFast()).isFalse();
        assertThat(stressed.scheduler().risingFast()).isTrue();
        assertThat(stressed.alerts()).anyMatch(alert -> alert.code().equals("scheduler_rising_fast"));
        executor.close();
        scheduler.shutdownNow();
        dataSource.close();
    }

    @Test
    void shouldCacheSnapshotOnlyWhenCacheTtlIsPositive() {
        PjbRuntimePressureProperties pressureProperties = new PjbRuntimePressureProperties();
        pressureProperties.setSnapshotCacheTtl(Duration.ofMillis(250));
        pressureProperties.setMinimumReadyAge(Duration.ZERO);
        isolateHostPressure(pressureProperties);
        PjbRuntimeAccelerationProperties accelerationProperties = new PjbRuntimeAccelerationProperties();
        PjbDatasourceBudgetProperties budgetProperties = new PjbDatasourceBudgetProperties();
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 8, true, Duration.ofSeconds(5), Duration.ofMillis(10));
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(12);
        dataSource.setMinimumIdle(2);
        ScheduledExecutorService scheduler = scheduler();
        PjbRuntimePressureService service = new PjbRuntimePressureService(
                pressureProperties,
                accelerationProperties,
                new PjbRuntimeSizingPolicy.Footprint(4, 2048),
                budgetProperties,
                Map.of("pjbIoExecutorService", executor),
                Map.of("pjbWriteDataSource", dataSource),
                scheduler
        );
        PjbRuntimePressureService.Snapshot first = service.snapshot();
        PjbRuntimePressureService.Snapshot second = service.snapshot();
        assertThat(second).isSameAs(first);
        executor.close();
        scheduler.shutdownNow();
        dataSource.close();
    }

    @Test
    void shouldDetectCriticalDatasourceRunaway() throws Exception {
        PjbRuntimePressureProperties pressureProperties = new PjbRuntimePressureProperties();
        pressureProperties.setSnapshotCacheTtl(Duration.ZERO);
        pressureProperties.setMinimumReadyAge(Duration.ZERO);
        isolateHostPressure(pressureProperties);
        pressureProperties.setPressureRisingFastDelta(100);
        pressureProperties.setDatasourceAwaitingThreshold(1);
        pressureProperties.setDatasourceAwaitingRisingFastDelta(1);
        pressureProperties.setCriticalDatasourceHeadroomThreshold(1);
        pressureProperties.setDatasourceTrendWindow(Duration.ofSeconds(30));
        PjbRuntimeAccelerationProperties accelerationProperties = new PjbRuntimeAccelerationProperties();
        accelerationProperties.setComponentRole("api");
        PjbDatasourceBudgetProperties budgetProperties = new PjbDatasourceBudgetProperties();
        PjbBoundedExecutorService executor = new PjbBoundedExecutorService("pjb-test-", 8, true, Duration.ofSeconds(5), Duration.ofMillis(10));
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(0);
        dataSource.setJdbcUrl("jdbc:h2:mem:pjb-pressure;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setConnectionTimeout(250);
        ScheduledExecutorService scheduler = scheduler();
        PjbRuntimePressureService service = new PjbRuntimePressureService(
                pressureProperties,
                accelerationProperties,
                new PjbRuntimeSizingPolicy.Footprint(4, 2048),
                budgetProperties,
                Map.of("pjbIoExecutorService", executor),
                Map.of("pjbWriteDataSource", dataSource),
                scheduler
        );
        PjbRuntimePressureService.Snapshot baseline = service.snapshot();
        Connection held = dataSource.getConnection();
        AtomicInteger failures = new AtomicInteger();
        Thread contender = Thread.ofPlatform().start(() -> {
            try {
                dataSource.getConnection().close();
            } catch (SQLException ex) {
                failures.incrementAndGet();
            }
        });
        contender.join(1000);
        PjbRuntimePressureService.Snapshot stressed = service.snapshot();
        held.close();
        assertThat(baseline.criticalDatasourceRunaway()).isFalse();
        assertThat(failures.get()).isGreaterThanOrEqualTo(0);
        assertThat(stressed.criticalDatasourceRunaway()).isTrue();
        assertThat(stressed.ready()).isFalse();
        assertThat(stressed.alerts()).anyMatch(alert -> alert.code().equals("critical_datasource_runaway") || alert.code().equals("critical_datasource_headroom_tight"));
        executor.close();
        scheduler.shutdownNow();
        dataSource.close();
    }

    private void isolateHostPressure(PjbRuntimePressureProperties properties) {
        properties.setHeapUsageThreshold(0.999d);
        properties.setMetaspaceUsageThreshold(0.999d);
        properties.setDirectBufferUsageThreshold(0.999d);
        properties.setGcPauseRatioThreshold(0.999d);
        properties.setGcAveragePauseMillisThreshold(Double.MAX_VALUE);
        properties.setGcCollectionTimeRisingFastMillis(Long.MAX_VALUE);
    }

    private ScheduledThreadPoolExecutor scheduler() {
        ThreadFactory threadFactory = Thread.ofPlatform().name("pjb-test-scheduler-", 0).factory();
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactory);
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
