package com.tcc.pjb.backend.core.judicial.sobrestamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaCommand;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaConsultaCommand;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaConsultaResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaResult;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaTimelineEntry;
import com.tcc.pjb.backend.core.judicial.sobrestamento.domain.SobrestamentoTemaTimelineResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SobrestamentoApplicationServiceTest {

    @Test
    void sobrestar_deveAuditarExecucaoManual() {
        SobrestamentoTemaService service = mock(SobrestamentoTemaService.class);
        AuditLedgerService audit = mock(AuditLedgerService.class);
        SobrestamentoApplicationService applicationService = new SobrestamentoApplicationService(service, audit);
        SobrestamentoTemaResult expected = new SobrestamentoTemaResult("Tema 101", 7, Instant.parse("2026-04-12T15:00:00Z"));
        when(service.sobrestar(new SobrestamentoTemaCommand("Tema 101"))).thenReturn(expected);

        SobrestamentoTemaResult result = applicationService.sobrestar("Tema 101");

        assertThat(result.totalAfetado()).isEqualTo(7);
        verify(audit).appendSafely(eq("SOBRESTAMENTO_MANUAL_RUN"), eq("TEMA"), eq("Tema 101"), eq("total=7"));
    }

    @Test
    void consulta_deveDelegarAoService() {
        SobrestamentoTemaService service = mock(SobrestamentoTemaService.class);
        AuditLedgerService audit = mock(AuditLedgerService.class);
        SobrestamentoApplicationService applicationService = new SobrestamentoApplicationService(service, audit);
        SobrestamentoTemaConsultaResult expected = new SobrestamentoTemaConsultaResult("Tema 99", 14L, "ATIVO");
        when(service.consultar(new SobrestamentoTemaConsultaCommand("Tema 99"))).thenReturn(expected);

        SobrestamentoTemaConsultaResult result = applicationService.consulta("Tema 99");

        assertThat(result.processosSobrestados()).isEqualTo(14L);
    }

    @Test
    void timeline_deveAuditarConsulta() {
        SobrestamentoTemaService service = mock(SobrestamentoTemaService.class);
        AuditLedgerService audit = mock(AuditLedgerService.class);
        SobrestamentoApplicationService applicationService = new SobrestamentoApplicationService(service, audit);
        SobrestamentoTemaTimelineResult expected = new SobrestamentoTemaTimelineResult(
                "Tema 44",
                List.of(new SobrestamentoTemaTimelineEntry("SOBRESTADO", Instant.parse("2026-04-12T15:10:00Z"), "processo=88"))
        );
        when(service.timeline("Tema 44")).thenReturn(expected);

        SobrestamentoTemaTimelineResult result = applicationService.timeline("Tema 44");

        assertThat(result.entries()).hasSize(1);
        verify(audit).appendSafely(eq("SOBRESTAMENTO_TIMELINE_QUERY"), eq("TEMA"), eq("Tema 44"), eq("eventos=1"));
    }
}
