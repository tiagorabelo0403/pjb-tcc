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

class SisbajudBloqueioServiceConsultasTest {

    @Test
    void shouldExposeConsultaSnapshotRetryAuditAndView() {
        SisbajudOperacaoRepository repository = mock(SisbajudOperacaoRepository.class);
        SisbajudOperacao entity = SisbajudOperacao.builder()
                .id(70L)
                .processoId(11L)
                .valorSolicitado(new BigDecimal("25.90"))
                .status("FAILED")
                .protocoloBacen("BACEN-70")
                .tentativas(2)
                .proximoRetryEm(Instant.parse("2026-04-11T13:00:00Z"))
                .confirmadoEm(Instant.parse("2026-04-11T12:00:00Z"))
                .build();
        when(repository.findById(70L)).thenReturn(Optional.of(entity));
        SisbajudBloqueioService service = new SisbajudBloqueioService(
                mock(ProcessoRepository.class),
                repository,
                (cpf, valor, oficio) -> { throw new UnsupportedOperationException(); },
                mock(CurrentUserService.class),
                mock(PjbAuthorizationService.class),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class));

        var consulta = service.consultar(new SisbajudConsultaCommand(70L));
        var snapshot = service.snapshot(70L);
        var retry = service.retrySnapshot(70L);
        var audit = service.auditSnapshot(70L);
        var view = service.view(70L);

        assertThat(consulta.status()).isEqualTo("FAILED");
        assertThat(snapshot.protocoloBacen()).isEqualTo("BACEN-70");
        assertThat(retry.tentativas()).isEqualTo(2);
        assertThat(audit.processoId()).isEqualTo(11L);
        assertThat(view.valorSolicitado()).isEqualByComparingTo("25.90");
    }
}
