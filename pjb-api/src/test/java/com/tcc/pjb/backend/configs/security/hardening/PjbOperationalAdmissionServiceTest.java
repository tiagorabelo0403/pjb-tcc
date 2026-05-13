package com.tcc.pjb.backend.configs.security.hardening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.platform.runtime.PjbKafkaPressureService;
import com.tcc.pjb.backend.platform.runtime.PjbLivePressureService;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimeDrainService;
import com.tcc.pjb.backend.platform.runtime.PjbRuntimePressureService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PjbOperationalAdmissionServiceTest {

    private PjbOperationalAdmissionProperties properties;
    private PjbRuntimePressureService pressureService;
    private PjbRuntimeDrainService drainService;
    private PjbLivePressureService livePressureService;
    private PjbKafkaPressureService kafkaPressureService;
    private PjbOperationalAdmissionService service;

    @BeforeEach
    void setUp() {
        properties = new PjbOperationalAdmissionProperties();
        properties.setDecisionTtl(Duration.ofMillis(1));
        pressureService = mock(PjbRuntimePressureService.class);
        drainService = mock(PjbRuntimeDrainService.class);
        livePressureService = mock(PjbLivePressureService.class);
        kafkaPressureService = mock(PjbKafkaPressureService.class);
        service = new PjbOperationalAdmissionService(properties, pressureService, drainService, livePressureService, kafkaPressureService);
        when(drainService.snapshot()).thenReturn(new PjbRuntimeDrainService.Snapshot(false, true, null, 0L, Duration.ofSeconds(20), "steady"));
        when(livePressureService.snapshot(false)).thenReturn(new PjbLivePressureService.Snapshot(0L, 0L, 0L, true, false, false, false, false, List.of("secretariat"), null));
        when(kafkaPressureService.snapshot(false)).thenReturn(new PjbKafkaPressureService.Snapshot(true, "", 1.0d, 0.0d, 0.0d, 0.0d, false, false));
    }

    @Test
    void shouldRejectWriteWhenCriticalDatasourceRunaway() {
        when(pressureService.snapshot()).thenReturn(snapshot(false, 78, true, false, false, false));
        PjbOperationalAdmissionService.Decision decision = service.evaluate("POST", "/api/v1/processual/acoes");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.hardRejection()).isTrue();
        assertThat(decision.code()).isEqualTo("CRITICAL_DATASOURCE_RUNAWAY");
    }

    @Test
    void shouldSoftRejectExpensiveRouteWhenSchedulerTrendingUp() {
        when(pressureService.snapshot()).thenReturn(snapshot(false, 49, false, true, false, false));
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/api/v1/juiz/painel");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.hardRejection()).isFalse();
        assertThat(decision.code()).isEqualTo("SCHEDULER_TRENDING_UP");
    }

    @Test
    void shouldBrownoutStreamWhenMemoryRunaway() {
        when(pressureService.snapshot()).thenReturn(snapshot(false, 64, false, false, false, true));
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/api/v1/juiz/votos/stream");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("CRITICAL_MEMORY_RUNAWAY");
        assertThat(decision.bucket()).startsWith("stream");
    }

    @Test
    void shouldRejectEventStreamWhenLiveSurges() {
        when(pressureService.snapshot()).thenReturn(snapshot(false, 35, false, false, false, false));
        when(livePressureService.snapshot(false)).thenReturn(new PjbLivePressureService.Snapshot(4500L, 700L, 600L, true, true, false, true, true, List.of("secretariat"), Instant.now()));
        PjbOperationalAdmissionService.OperationShape shape = new PjbOperationalAdmissionService.OperationShape(0L, 0, 0L, true);
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/api/v1/juiz/votos/stream", shape);
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("LIVE_STREAM_SURGE");
    }

    @Test
    void shouldRejectExpensiveRouteWhenCriticalGcPressure() {
        when(pressureService.snapshot()).thenReturn(snapshotWithCriticalGcPressure());
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/api/v1/juiz/export/relatorio");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("CRITICAL_GC_PRESSURE");
    }

    @Test
    void shouldRejectVeryLargeWriteWhenPressureIsRising() {
        when(pressureService.snapshot()).thenReturn(snapshot(false, 58, false, false, false, false));
        PjbOperationalAdmissionService.OperationShape shape = new PjbOperationalAdmissionService.OperationShape(32L * 1024L * 1024L, 0, 0L, false);
        PjbOperationalAdmissionService.Decision decision = service.evaluate("POST", "/api/v1/peticionamento/protocolar", shape);
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("LOW_PRIORITY_BROWNOUT");
    }


    @Test
    void shouldPreserveHighPriorityReadUnderSoftPressure() {
        when(pressureService.snapshot()).thenReturn(snapshot(false, 60, false, false, false, false));
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/api/v1/cidadao/painel/processos");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.code()).isEqualTo("PRIORITY_PRESERVED");
        assertThat(decision.priority()).isEqualTo(PjbOperationalAdmissionService.RoutePriority.CRITICAL);
    }

    @Test
    void shouldRejectLowPriorityExportEarlier() {
        when(pressureService.snapshot()).thenReturn(snapshot(false, 48, false, false, false, false));
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/api/v1/juiz/export/relatorio");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("LOW_PRIORITY_BROWNOUT");
        assertThat(decision.priority()).isEqualTo(PjbOperationalAdmissionService.RoutePriority.LOW);
    }


    @Test
    void shouldRejectLowPriorityWhenApiReservedLanesAreHot() {
        when(pressureService.snapshot()).thenReturn(snapshotWithLaneOverload("api", "external-io", false));
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/api/v1/juiz/export/relatorio");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("RESERVED_LANE_BUDGET_BREACH");
        assertThat(decision.priority()).isEqualTo(PjbOperationalAdmissionService.RoutePriority.LOW);
    }

    @Test
    void shouldPreserveCriticalReadEvenWhenReservedLaneIsHot() {
        when(pressureService.snapshot()).thenReturn(snapshotWithLaneOverload("api", "external-io", true));
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/api/v1/cidadao/painel/processos");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.code()).isEqualTo("PRIORITY_PRESERVED");
        assertThat(decision.priority()).isEqualTo(PjbOperationalAdmissionService.RoutePriority.CRITICAL);
    }


    @Test
    void shouldRejectLowPriorityWhenReservedLaneBudgetIsTightBeforeCollapse() {
        when(pressureService.snapshot()).thenReturn(snapshotWithLaneBudgetBreach("api"));
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/api/v1/juiz/export/relatorio");
        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("RESERVED_LANE_BUDGET_BREACH");
        assertThat(decision.hardRejection()).isFalse();
    }

    @Test
    void shouldAllowExemptRoute() {
        PjbOperationalAdmissionService.Decision decision = service.evaluate("GET", "/startupz");
        assertThat(decision.allowed()).isTrue();
        assertThat(decision.code()).isEqualTo("EXEMPT");
    }


    private PjbRuntimePressureService.Snapshot snapshotWithLaneBudgetBreach(String role) {
        PjbRuntimePressureService.ExecutorPressure externalIo = new PjbRuntimePressureService.ExecutorPressure(
                "pjb-external-io-executor",
                "external-io",
                "pjb-http-",
                48,
                39,
                0.81d,
                34.0d,
                0L,
                0,
                null,
                true,
                true,
                false,
                true
        );
        PjbRuntimePressureService.ExecutorPressure live = new PjbRuntimePressureService.ExecutorPressure(
                "pjb-live-executor",
                "live",
                "pjb-live-",
                32,
                27,
                0.84d,
                28.0d,
                0L,
                0,
                null,
                true,
                true,
                false,
                true
        );
        PjbRuntimePressureService.DatasourcePressure write = new PjbRuntimePressureService.DatasourcePressure(
                "writeDataSource",
                "write",
                6,
                10,
                0,
                24,
                24,
                18,
                0.25d,
                true,
                false,
                0,
                0.01d,
                false,
                false,
                false
        );
        PjbRuntimePressureService.MemoryPressure memory = new PjbRuntimePressureService.MemoryPressure(
                960,
                4096,
                0.23d,
                24,
                false,
                false,
                72,
                512,
                0.14d,
                28,
                5,
                0.01d,
                false,
                false
        );
        return new PjbRuntimePressureService.Snapshot(
                true,
                false,
                36,
                64,
                role,
                8,
                4096,
                Instant.now(),
                80000L,
                List.of(externalIo, live),
                List.of(write),
                new PjbRuntimePressureService.SchedulerPressure("pjbTimeoutScheduler", 2, 1, 1, 1.0d, 0, false, false, false),
                memory,
                gc(false),
                new PjbRuntimePressureService.PressureTrend(32, 4, true, false, false, null),
                List.of()
        );
    }

    private PjbRuntimePressureService.Snapshot snapshotWithLaneOverload(String role,
                                                                        String laneName,
                                                                        boolean mildPressure) {
        PjbRuntimePressureService.ExecutorPressure executor = new PjbRuntimePressureService.ExecutorPressure(
                "pjb-" + laneName + "-executor",
                laneName,
                "pjb-" + laneName + "-",
                32,
                mildPressure ? 24 : 31,
                mildPressure ? 0.75d : 0.97d,
                mildPressure ? 18.0d : 280.0d,
                mildPressure ? 0L : 12L,
                mildPressure ? 0 : 3,
                Instant.now(),
                true,
                true,
                true,
                true
        );
        PjbRuntimePressureService.DatasourcePressure write = new PjbRuntimePressureService.DatasourcePressure(
                "writeDataSource",
                "write",
                4,
                10,
                0,
                24,
                24,
                20,
                0.16d,
                true,
                false,
                0,
                0.01d,
                false,
                false,
                false
        );
        PjbRuntimePressureService.MemoryPressure memory = new PjbRuntimePressureService.MemoryPressure(
                900,
                4096,
                0.22d,
                32,
                false,
                false,
                64,
                512,
                0.12d,
                24,
                4,
                0.01d,
                false,
                false
        );
        return new PjbRuntimePressureService.Snapshot(
                mildPressure,
                false,
                mildPressure ? 34 : 24,
                mildPressure ? 66 : 76,
                role,
                8,
                4096,
                Instant.now(),
                120000L,
                List.of(executor),
                List.of(write),
                new PjbRuntimePressureService.SchedulerPressure("pjbTimeoutScheduler", 0, 0, 1, 0.0d, 0, false, false, false),
                memory,
                gc(false),
                new PjbRuntimePressureService.PressureTrend(mildPressure ? 30 : 20, mildPressure ? 4 : 4, true, false, false, null),
                List.of()
        );
    }

    private PjbRuntimePressureService.GcPressure gc(boolean critical) {
        return new PjbRuntimePressureService.GcPressure(
                120,
                critical ? 4200 : 900,
                critical ? 12 : 2,
                critical ? 640 : 40,
                critical ? 0.18d : 0.01d,
                critical ? 220.0d : 20.0d,
                true,
                critical,
                critical,
                critical,
                critical
        );
    }

    private PjbRuntimePressureService.Snapshot snapshotWithCriticalGcPressure() {
        PjbRuntimePressureService.DatasourcePressure write = new PjbRuntimePressureService.DatasourcePressure(
                "writeDataSource",
                "write",
                4,
                10,
                0,
                24,
                24,
                20,
                0.16d,
                true,
                false,
                0,
                0.01d,
                false,
                false,
                false
        );
        PjbRuntimePressureService.MemoryPressure memory = new PjbRuntimePressureService.MemoryPressure(
                900,
                4096,
                0.22d,
                16,
                false,
                false,
                72,
                512,
                0.14d,
                28,
                4,
                0.01d,
                false,
                false
        );
        return new PjbRuntimePressureService.Snapshot(
                false,
                false,
                74,
                18,
                "api",
                8,
                4096,
                Instant.now(),
                100000L,
                List.of(),
                List.of(write),
                new PjbRuntimePressureService.SchedulerPressure("pjbTimeoutScheduler", 0, 0, 1, 0.0d, 0, false, false, false),
                memory,
                gc(true),
                new PjbRuntimePressureService.PressureTrend(58, 16, true, true, true, Instant.now().minusSeconds(90)),
                List.of()
        );
    }

    private PjbRuntimePressureService.Snapshot snapshot(boolean warmingUp,
                                                        int pressureScore,
                                                        boolean criticalDatasourceRunaway,
                                                        boolean schedulerTrending,
                                                        boolean sustained,
                                                        boolean criticalMemoryRunaway) {
        PjbRuntimePressureService.SchedulerPressure scheduler = new PjbRuntimePressureService.SchedulerPressure(
                "pjbTimeoutScheduler",
                schedulerTrending ? 320 : 10,
                schedulerTrending ? 8 : 1,
                8,
                schedulerTrending ? 1.0d : 0.12d,
                schedulerTrending ? 90 : 0,
                schedulerTrending,
                sustained,
                schedulerTrending
        );
        PjbRuntimePressureService.DatasourcePressure write = new PjbRuntimePressureService.DatasourcePressure(
                "writeDataSource",
                "write",
                criticalDatasourceRunaway ? 23 : 4,
                criticalDatasourceRunaway ? 1 : 10,
                criticalDatasourceRunaway ? 9 : 0,
                24,
                24,
                criticalDatasourceRunaway ? 1 : 20,
                criticalDatasourceRunaway ? 0.96d : 0.16d,
                true,
                criticalDatasourceRunaway,
                criticalDatasourceRunaway ? 5 : 0,
                criticalDatasourceRunaway ? 0.15d : 0.01d,
                criticalDatasourceRunaway,
                criticalDatasourceRunaway,
                criticalDatasourceRunaway
        );
        PjbRuntimePressureService.MemoryPressure memory = new PjbRuntimePressureService.MemoryPressure(
                criticalMemoryRunaway ? 3500 : 800,
                4096,
                criticalMemoryRunaway ? 0.95d : 0.20d,
                criticalMemoryRunaway ? 220 : 8,
                criticalMemoryRunaway,
                sustained && criticalMemoryRunaway,
                criticalMemoryRunaway ? 320 : 64,
                512,
                criticalMemoryRunaway ? 0.62d : 0.12d,
                criticalMemoryRunaway ? 420 : 24,
                criticalMemoryRunaway ? 96 : 4,
                criticalMemoryRunaway ? 0.18d : 0.01d,
                criticalMemoryRunaway,
                criticalMemoryRunaway
        );
        return new PjbRuntimePressureService.Snapshot(
                !warmingUp && !criticalDatasourceRunaway && !schedulerTrending && !criticalMemoryRunaway,
                warmingUp,
                pressureScore,
                100 - pressureScore,
                "api",
                8,
                4096,
                Instant.now(),
                90000L,
                List.of(),
                List.of(write),
                scheduler,
                memory,
                gc(false),
                new PjbRuntimePressureService.PressureTrend(pressureScore - 10, 10, true, pressureScore >= 55, sustained, sustained ? Instant.now().minusSeconds(120) : null),
                List.of()
        );
    }
}
