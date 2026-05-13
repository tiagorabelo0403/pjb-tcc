package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SisbajudApplicationServiceTest {

    @Test
    void bloquear_deveAuditarResultadoManual() {
        SisbajudBloqueioService bloqueioService = mock(SisbajudBloqueioService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(bloqueioService.solicitarBloqueio(new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioRequest(8L, "123", BigDecimal.TEN, "OF-1", false), "trail-1"))
                .thenReturn(SisbajudBloqueioResult.success(12L, "PROTO-1", "OK"));
        SisbajudApplicationService applicationService = new SisbajudApplicationService(bloqueioService, auditLedgerService);

        var result = applicationService.bloquear(8L, "123", BigDecimal.TEN, "OF-1", "trail-1", false);

        assertThat(result.success()).isTrue();
        verify(auditLedgerService).appendSafely(eq("SISBAJUD_BLOQUEIO_MANUAL"), eq("PROCESSO"), eq("8"), isNull(), eq("success=true"));
    }
}
