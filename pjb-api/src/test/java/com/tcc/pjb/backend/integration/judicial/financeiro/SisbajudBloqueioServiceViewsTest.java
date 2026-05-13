package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudConsultaCommand;
import com.tcc.pjb.backend.model.entity.financeiro.SisbajudOperacao;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SisbajudOperacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SisbajudBloqueioServiceViewsTest {

    @Test
    void shouldExposeConsultaSnapshotRetryAuditAndView() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        SisbajudOperacaoRepository operacaoRepository = mock(SisbajudOperacaoRepository.class);
        SisbajudOperacao entity = SisbajudOperacao.builder()
                .id(11L)
                .processoId(7L)
                .valorSolicitado(new BigDecimal("33.00"))
                .protocoloBacen("BACEN-33")
                .status("FAILED")
                .tentativas(2)
                .proximoRetryEm(Instant.parse("2026-04-11T12:00:00Z"))
                .confirmadoEm(Instant.parse("2026-04-11T11:00:00Z"))
                .build();
        when(operacaoRepository.findById(11L)).thenReturn(Optional.of(entity));
        SisbajudBloqueioService service = new SisbajudBloqueioService(
                processoRepository,
                operacaoRepository,
                (cpf, valor, oficio) -> { throw new UnsupportedOperationException(); },
                mock(CurrentUserService.class),
                mock(PjbAuthorizationService.class),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class));

        var consulta = service.consultar(new SisbajudConsultaCommand(11L));
        var snapshot = service.snapshot(11L);
        var retry = service.retrySnapshot(11L);
        var audit = service.auditSnapshot(11L);
        var view = service.view(11L);

        assertThat(consulta.status()).isEqualTo("FAILED");
        assertThat(snapshot.protocoloBacen()).isEqualTo("BACEN-33");
        assertThat(retry.tentativas()).isEqualTo(2);
        assertThat(audit.confirmadoEm()).isEqualTo(Instant.parse("2026-04-11T11:00:00Z"));
        assertThat(view.valorSolicitado()).isEqualByComparingTo("33.00");
    }
}
