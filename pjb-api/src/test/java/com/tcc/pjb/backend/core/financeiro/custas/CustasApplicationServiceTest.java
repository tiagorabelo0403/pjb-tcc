package com.tcc.pjb.backend.core.financeiro.custas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.financeiro.custas.domain.CustaConsultaTimelineResult;
import com.tcc.pjb.backend.core.financeiro.custas.domain.CustaTimelineEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustasApplicationServiceTest {

    @Test
    void gerar_deveAuditarCriacaoManual() {
        CustaJudicialService custaJudicialService = mock(CustaJudicialService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(custaJudicialService.gerarCustas(new com.tcc.pjb.backend.core.financeiro.custas.domain.GerarCustaJudicialCommand(3L, "CUSTAS_INICIAIS", BigDecimal.TEN)))
                .thenReturn(com.tcc.pjb.backend.core.financeiro.custas.domain.CustaJudicialResult.isento(9L, "gratuidade"));
        CustasApplicationService applicationService = new CustasApplicationService(custaJudicialService, auditLedgerService);

        var result = applicationService.gerar(3L, "CUSTAS_INICIAIS", BigDecimal.TEN);

        assertThat(result.isento()).isTrue();
        verify(auditLedgerService).appendSafely(eq("CUSTA_GERACAO_MANUAL"), eq("PROCESSO"), eq("3"), isNull(), eq("isento=true"));
    }

    @Test
    void timeline_deveAuditarQuantidade() {
        CustaJudicialService custaJudicialService = mock(CustaJudicialService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(custaJudicialService.consultarTimeline(new com.tcc.pjb.backend.core.financeiro.custas.domain.CustaConsultaTimelineCommand(9L)))
                .thenReturn(new CustaConsultaTimelineResult(9L, List.of(
                        new CustaTimelineEntry("GERADA", Instant.parse("2026-04-11T12:00:00Z"), "PENDENTE"),
                        new CustaTimelineEntry("CONSULTADA", Instant.parse("2026-04-11T12:10:00Z"), "PENDENTE")
                )));
        CustasApplicationService applicationService = new CustasApplicationService(custaJudicialService, auditLedgerService);

        var result = applicationService.timeline(9L);

        assertThat(result.entries()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("CUSTA_TIMELINE_QUERY"), eq("CUSTA"), eq("9"), isNull(), eq("entries=2"));
    }
}
