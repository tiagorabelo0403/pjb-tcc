package com.tcc.pjb.backend.core.dje;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import com.tcc.pjb.backend.core.dje.DjeTestFactory;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.dje.domain.DjeEdicaoMetricsView;
import com.tcc.pjb.backend.core.dje.domain.DjeLifecycleExecutionSummary;
import com.tcc.pjb.backend.core.dje.domain.DjeTimelineEntry;
import com.tcc.pjb.backend.core.dje.domain.DjeTimelineView;
import com.tcc.pjb.backend.core.dje.domain.DjeTribunalHealthView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DjeApplicationServiceTest {

    @Test
    void lifecycleRun_deveUsarBatchPadraoEAuditarExecucao() {
        DjePublicacaoService publicacaoService = mock(DjePublicacaoService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(publicacaoService.executarLifecycle(LocalDate.of(2026, 4, 11), 30))
                .thenReturn(new DjeLifecycleExecutionSummary(2, 3));
        DjeApplicationService applicationService = new DjeApplicationService(
                publicacaoService,
                DjeTestFactory.propertiesForTest(true, 300000, 30),
                auditLedgerService);

        var result = applicationService.lifecycleRun(LocalDate.of(2026, 4, 11), null);

        assertThat(result.publicadasConsolidadas()).isEqualTo(2);
        assertThat(result.partesNotificadas()).isEqualTo(3);
        verify(auditLedgerService).appendSafely(
                eq("DJE_LIFECYCLE_RUN"),
                eq("DJE"),
                eq("2026-04-11"),
                isNull(),
                eq("publicadas=2 notificadas=3 batchSize=30"));
    }

    @Test
    void timeline_deveDelegarEAuditarQuantidadeDeEntradas() {
        DjePublicacaoService publicacaoService = mock(DjePublicacaoService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(publicacaoService.timeline(org.mockito.ArgumentMatchers.any(com.tcc.pjb.backend.core.dje.domain.DjeTimelineQuery.class))).thenReturn(new DjeTimelineView(
                15L,
                List.of(
                        new DjeTimelineEntry("CRIADO", Instant.parse("2026-04-11T12:00:00Z"), "SENTENCA"),
                        new DjeTimelineEntry("ENVIADO", Instant.parse("2026-04-11T12:05:00Z"), "ED-1"))));
        DjeApplicationService applicationService = new DjeApplicationService(
                publicacaoService,
                DjeTestFactory.propertiesForTest(true, 300000, 25),
                auditLedgerService);

        var result = applicationService.timeline(15L);

        assertThat(result.entries()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("DJE_TIMELINE_QUERY"), eq("DJE"), eq("15"), isNull(), eq("entries=2"));
    }

    @Test
    void tribunalHealth_deveNormalizarReferenciaEAuditarResumo() {
        DjePublicacaoService publicacaoService = mock(DjePublicacaoService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(publicacaoService.tribunalHealthView("TJCE")).thenReturn(new DjeTribunalHealthView("TJCE", "OK", "total=4"));
        DjeApplicationService applicationService = new DjeApplicationService(
                publicacaoService,
                DjeTestFactory.propertiesForTest(true, 300000, 25),
                auditLedgerService);

        var result = applicationService.tribunalHealth("tjce");

        assertThat(result.reference()).isEqualTo("TJCE");
        verify(auditLedgerService).appendSafely(eq("DJE_TRIBUNAL_HEALTH_QUERY"), eq("DJE_TRIBUNAL"), eq("TJCE"), isNull(), eq("total=4"));
    }

    @Test
    void edicaoMetrics_deveAuditarPublicacoesDaEdicao() {
        DjePublicacaoService publicacaoService = mock(DjePublicacaoService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(publicacaoService.edicaoMetrics("ED-2026-10")).thenReturn(new DjeEdicaoMetricsView("ED-2026-10", 8, 1, 2));
        DjeApplicationService applicationService = new DjeApplicationService(
                publicacaoService,
                DjeTestFactory.propertiesForTest(true, 300000, 25),
                auditLedgerService);

        var result = applicationService.edicaoMetrics("ed-2026-10");

        assertThat(result.publicacoes()).isEqualTo(8);
        verify(auditLedgerService).appendSafely(eq("DJE_EDICAO_METRICS_QUERY"), eq("DJE_EDICAO"), eq("ED-2026-10"), isNull(), eq("publicacoes=8"));
    }
}
