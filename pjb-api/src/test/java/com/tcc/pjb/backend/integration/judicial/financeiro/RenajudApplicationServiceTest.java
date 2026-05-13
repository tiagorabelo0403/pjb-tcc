package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResult;
import com.tcc.pjb.backend.model.entity.financeiro.RenajudRestricao;
import com.tcc.pjb.backend.model.repository.RenajudRestricaoRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RenajudApplicationServiceTest {

    @Test
    void restringir_deveAuditarExecucaoManual() {
        RenajudRestricaoService service = mock(RenajudRestricaoService.class);
        RenajudRestricaoRepository repository = mock(RenajudRestricaoRepository.class);
        AuditLedgerService audit = mock(AuditLedgerService.class);
        when(service.solicitarRestricao(new com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoRequest(8L, "RESTRICAO", "ABC1234", null), "trail-1"))
                .thenReturn(RenajudRestricaoResult.success(12L, "PROTO-1", "OK"));
        RenajudApplicationService applicationService = new RenajudApplicationService(service, repository, audit);

        var result = applicationService.restringir(8L, "ABC1234", null, "RESTRICAO", "trail-1");

        assertThat(result.success()).isTrue();
        verify(audit).appendSafely(eq("RENAJUD_RESTRICAO_MANUAL"), eq("PROCESSO"), eq("8"), isNull(), eq("success=true"));
    }

    @Test
    void snapshot_deveMapearEntidade() {
        RenajudRestricaoService service = mock(RenajudRestricaoService.class);
        RenajudRestricaoRepository repository = mock(RenajudRestricaoRepository.class);
        AuditLedgerService audit = mock(AuditLedgerService.class);
        when(repository.findById(12L)).thenReturn(Optional.of(RenajudRestricao.builder()
                .id(12L)
                .processoId(8L)
                .placa("ABC1234")
                .status("CONFIRMED")
                .confirmadoEm(Instant.parse("2026-04-12T12:00:00Z"))
                .build()));
        RenajudApplicationService applicationService = new RenajudApplicationService(service, repository, audit);

        var result = applicationService.snapshot(12L);

        assertThat(result.restricaoId()).isEqualTo(12L);
        assertThat(result.status()).isEqualTo("CONFIRMED");
    }
}
