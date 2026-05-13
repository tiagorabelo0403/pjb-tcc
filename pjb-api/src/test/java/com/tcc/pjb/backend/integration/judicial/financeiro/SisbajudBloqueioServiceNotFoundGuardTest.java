package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudConsultaCommand;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SisbajudOperacaoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SisbajudBloqueioServiceNotFoundGuardTest {

    @Test
    void shouldThrowWhenConsultingUnknownOperationAcrossViews() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        SisbajudOperacaoRepository operacaoRepository = mock(SisbajudOperacaoRepository.class);
        when(operacaoRepository.findById(404L)).thenReturn(Optional.empty());

        SisbajudBloqueioService service = new SisbajudBloqueioService(
                processoRepository,
                operacaoRepository,
                mock(SisbajudHttpClient.class),
                mock(CurrentUserService.class),
                mock(PjbAuthorizationService.class),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class));

        assertThatThrownBy(() -> service.consultar(new SisbajudConsultaCommand(404L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Operação SISBAJUD não encontrada");
        assertThatThrownBy(() -> service.snapshot(404L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.retrySnapshot(404L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.auditSnapshot(404L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.view(404L)).isInstanceOf(IllegalArgumentException.class);
    }
}
