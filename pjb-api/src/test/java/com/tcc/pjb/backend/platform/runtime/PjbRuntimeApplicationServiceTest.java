package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbRuntimeApplicationServiceTest {

    @Test
    void pressure_deveMaterializarSnapshotEAuditarConsulta() {
        PjbRuntimePressureService pressureService = mock(PjbRuntimePressureService.class);
        PjbRuntimeDrainService drainService = mock(PjbRuntimeDrainService.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        PjbRuntimeAccelerationProperties properties = new PjbRuntimeAccelerationProperties();
        properties.setComponentRole("api");
        when(pressureService.snapshot()).thenReturn(new PjbRuntimePressureService.Snapshot(
                true,
                false,
                12,
                88,
                "api",
                8,
                2048L,
                Instant.parse("2026-04-11T12:00:00Z"),
                5000L,
                List.of(),
                List.of(),
                null,
                null,
                null,
                new PjbRuntimePressureService.PressureTrend(10, 2, true, true, false, null),
                List.of()));
        PjbRuntimeApplicationService applicationService = new PjbRuntimeApplicationService(
                pressureService,
                drainService,
                new PjbRuntimeSizingPolicy.Footprint(8, 2048L),
                properties,
                rawPolicy,
                auditLedgerService);

        var result = applicationService.pressure();

        assertThat(result.pressureScore()).isEqualTo(12);
        assertThat(result.trend()).isEqualTo("rising_fast");
        verify(auditLedgerService).appendSafely(eq("RUNTIME_PRESSURE_QUERY"), eq("RUNTIME"), eq("api"), isNull(), eq("score=12 headroom=88"));
    }

    @Test
    void beginDrain_deveDelegarEAuditar() {
        PjbRuntimePressureService pressureService = mock(PjbRuntimePressureService.class);
        PjbRuntimeDrainService drainService = mock(PjbRuntimeDrainService.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(drainService.snapshot()).thenReturn(new PjbRuntimeDrainService.Snapshot(true, false, Instant.parse("2026-04-11T12:00:00Z"), 3000L, Duration.ofSeconds(20), "manual-drain"));
        PjbRuntimeApplicationService applicationService = new PjbRuntimeApplicationService(
                pressureService,
                drainService,
                new PjbRuntimeSizingPolicy.Footprint(6, 1536L),
                new PjbRuntimeAccelerationProperties(),
                rawPolicy,
                auditLedgerService);

        var result = applicationService.beginDrain("manual-drain");

        assertThat(result.draining()).isTrue();
        verify(drainService).beginDrain("manual-drain");
        verify(auditLedgerService).appendSafely(eq("RUNTIME_DRAIN_BEGIN"), eq("RUNTIME"), eq("manual-drain"), isNull(), eq("draining=true"));
    }
}
