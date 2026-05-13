package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioRequest;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudHttpResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SisbajudOperacaoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SisbajudBloqueioServiceSuccessPathTest {

    @Test
    void shouldReturnConfirmedResultWhenRemoteRequestSucceeds() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        SisbajudOperacaoRepository operacaoRepository = mock(SisbajudOperacaoRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        when(processoRepository.findById(7L)).thenReturn(Optional.of(Processo.builder().id(7L).build()));
        when(currentUserService.getRequired()).thenReturn(Usuario.builder().id(99L).build());
        when(operacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SisbajudBloqueioService service = new SisbajudBloqueioService(
                processoRepository,
                operacaoRepository,
                (cpf, valor, oficio) -> new SisbajudHttpResponse("BACEN-1", "ok"),
                currentUserService,
                authorizationService,
                mock(AuditLedgerService.class),
                rawPolicy);

        var result = service.solicitarBloqueio(new SisbajudBloqueioRequest(7L, "12345678901", new BigDecimal("10.00"), "OF-1", false), "AUTHZ-OK");

        assertThat(result.success()).isTrue();
        assertThat(result.protocolo()).isEqualTo("BACEN-1");
        verify(rawPolicy).markWrite();
    }
}
