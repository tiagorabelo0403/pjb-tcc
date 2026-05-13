package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResult;
import com.tcc.pjb.backend.model.entity.financeiro.InfojudConsulta;
import com.tcc.pjb.backend.model.repository.InfojudConsultaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InfojudApplicationServiceTest {

    @Test
    void consultar_deveAuditarExecucaoManual() {
        InfojudConsultaService service = mock(InfojudConsultaService.class);
        InfojudConsultaRepository repository = mock(InfojudConsultaRepository.class);
        AuditLedgerService audit = mock(AuditLedgerService.class);
        when(service.consultar(new com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaRequest(8L, "12345678901", false), "trail-1"))
                .thenReturn(InfojudConsultaResult.success(15L, "PROTO-2", "OK"));
        InfojudApplicationService applicationService = new InfojudApplicationService(service, repository, audit);

        var result = applicationService.consultar(8L, "12345678901", "trail-1", false);

        assertThat(result.success()).isTrue();
        verify(audit).appendSafely(eq("INFOJUD_CONSULTA_MANUAL"), eq("PROCESSO"), eq("8"), isNull(), eq("success=true"));
    }

    @Test
    void snapshot_deveMapearEntidade() {
        InfojudConsultaService service = mock(InfojudConsultaService.class);
        InfojudConsultaRepository repository = mock(InfojudConsultaRepository.class);
        AuditLedgerService audit = mock(AuditLedgerService.class);
        when(repository.findById(15L)).thenReturn(Optional.of(InfojudConsulta.builder()
                .id(15L)
                .processoId(8L)
                .cpfCnpjConsultado("12345678901")
                .status("CONFIRMED")
                .confirmadoEm(Instant.parse("2026-04-12T12:30:00Z"))
                .build()));
        InfojudApplicationService applicationService = new InfojudApplicationService(service, repository, audit);

        var result = applicationService.snapshot(15L);

        assertThat(result.consultaId()).isEqualTo(15L);
        assertThat(result.status()).isEqualTo("CONFIRMED");
    }
}
