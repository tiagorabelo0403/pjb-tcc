package com.tcc.pjb.backend.core.financeiro.trabalhista;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.GruTrabalhistaResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaConsultaTimelineResult;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaTimelineAuditSnapshot;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.TrabalhistaTimelineEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrabalhistaApplicationServiceTest {

    @Test
    void gerarGru_deveDelegarEAuditar() {
        WorkflowTrabalhistaService workflowTrabalhistaService = mock(WorkflowTrabalhistaService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(workflowTrabalhistaService.gerarGruRecursal(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new GruTrabalhistaResult(9L, "123", "456", LocalDate.of(2026, 5, 1)));
        TrabalhistaApplicationService applicationService = new TrabalhistaApplicationService(workflowTrabalhistaService, auditLedgerService);

        var result = applicationService.gerarGru(1L, "PREPARO_RECURSAL", java.math.BigDecimal.valueOf(500));

        assertThat(result.gruId()).isEqualTo(9L);
        verify(auditLedgerService).appendSafely(eq("TRABALHISTA_SURFACE_GRU"), eq("PROCESSO"), eq("1"), isNull(), eq("gruId=9"));
    }

    @Test
    void timeline_deveAuditarQuantidadeDeEventos() {
        WorkflowTrabalhistaService workflowTrabalhistaService = mock(WorkflowTrabalhistaService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(workflowTrabalhistaService.timeline(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new TrabalhistaConsultaTimelineResult(7L, List.of(
                        new TrabalhistaTimelineEntry("GRU", "PENDENTE"),
                        new TrabalhistaTimelineEntry("DEPOSITO", "CONFIRMADO"))));
        TrabalhistaApplicationService applicationService = new TrabalhistaApplicationService(workflowTrabalhistaService, auditLedgerService);

        var result = applicationService.timeline(7L);

        assertThat(result.entries()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("TRABALHISTA_TIMELINE_QUERY"), eq("PROCESSO"), eq("7"), isNull(), eq("entries=2"));
    }

    @Test
    void timelineAudit_deveAuditarSnapshot() {
        WorkflowTrabalhistaService workflowTrabalhistaService = mock(WorkflowTrabalhistaService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(workflowTrabalhistaService.timelineAudit(5L)).thenReturn(new TrabalhistaTimelineAuditSnapshot(5L, 3, true, true));
        TrabalhistaApplicationService applicationService = new TrabalhistaApplicationService(workflowTrabalhistaService, auditLedgerService);

        var result = applicationService.timelineAudit(5L);

        assertThat(result.totalEventos()).isEqualTo(3);
        verify(auditLedgerService).appendSafely(eq("TRABALHISTA_TIMELINE_AUDIT_QUERY"), eq("PROCESSO"), eq("5"), isNull(), eq("entries=3"));
    }
}
