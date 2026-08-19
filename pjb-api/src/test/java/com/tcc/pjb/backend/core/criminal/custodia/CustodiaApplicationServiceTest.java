package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.criminal.custodia.domain.AudienciaCustodiaResult;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineResult;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaPendenteView;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaTimelineEntry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustodiaApplicationServiceTest {

    @Test
    void registrarPrisao_deveAuditar() {
        AudienciaCustodiaService audienciaCustodiaService = mock(AudienciaCustodiaService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(audienciaCustodiaService.registrarPrisao(new com.tcc.pjb.backend.core.criminal.custodia.domain.RegistrarPrisaoCommand(4L, "João", "123", null)))
                .thenReturn(new AudienciaCustodiaResult(7L, Instant.parse("2026-04-12T12:00:00Z")));
        CustodiaApplicationService applicationService = new CustodiaApplicationService(audienciaCustodiaService, auditLedgerService);

        var result = applicationService.registrarPrisao(4L, "João", "123", null);

        assertThat(result.custodiaId()).isEqualTo(7L);
        verify(auditLedgerService).appendSafely(eq("CUSTODIA_PRISAO_MANUAL"), eq("PROCESSO"), eq("4"), isNull(), eq("João"));
    }

    @Test
    void timeline_deveAuditarConsulta() {
        AudienciaCustodiaService audienciaCustodiaService = mock(AudienciaCustodiaService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(audienciaCustodiaService.consultarTimeline(new com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineCommand(7L)))
                .thenReturn(new CustodiaConsultaTimelineResult(7L, List.of(
                        new CustodiaTimelineEntry("PENDENTE", Instant.parse("2026-04-11T12:00:00Z"), "PRISAO"),
                        new CustodiaTimelineEntry("REALIZADA", Instant.parse("2026-04-11T13:00:00Z"), "LIBERDADE")
                )));
        CustodiaApplicationService applicationService = new CustodiaApplicationService(audienciaCustodiaService, auditLedgerService);

        var result = applicationService.timeline(7L);

        assertThat(result.entries()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("CUSTODIA_TIMELINE_QUERY"), eq("CUSTODIA"), eq("7"), isNull(), eq("entries=2"));
    }

    @Test
    void pendentes_delegaParaOServicoDeDominio() {
        AudienciaCustodiaService audienciaCustodiaService = mock(AudienciaCustodiaService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(audienciaCustodiaService.pendentes()).thenReturn(List.of(
                new CustodiaPendenteView(1L, 10L, "Fulano", Instant.parse("2026-04-11T08:00:00Z"), Instant.parse("2026-04-12T08:00:00Z"), true)));
        CustodiaApplicationService applicationService = new CustodiaApplicationService(audienciaCustodiaService, auditLedgerService);

        var result = applicationService.pendentes();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().vencida()).isTrue();
    }
}
