package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SisbajudOperacaoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SisbajudBloqueioServiceFailureModesTest {

    @Test
    void shouldRejectMissingAuthzTrailId() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        SisbajudOperacaoRepository operacaoRepository = mock(SisbajudOperacaoRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        when(processoRepository.findById(9L)).thenReturn(Optional.of(Processo.builder().id(9L).build()));
        when(currentUserService.getRequired()).thenReturn(Usuario.builder().id(5L).build());
        SisbajudBloqueioService service = new SisbajudBloqueioService(processoRepository, operacaoRepository, (cpf, valor, oficio) -> new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudHttpResponse("PROTO", "ok"), currentUserService, authorizationService, mock(AuditLedgerService.class), new ReadAfterWriteConsistencyPolicy());
        assertThatThrownBy(() -> service.solicitarBloqueio(new SisbajudBloqueioRequest(9L, "123", new BigDecimal("10.00"), "OF", false), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("authzTrailId");
    }

    @Test
    void shouldReturnFailedResultWhenHttpClientThrows() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        SisbajudOperacaoRepository operacaoRepository = mock(SisbajudOperacaoRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        when(processoRepository.findById(9L)).thenReturn(Optional.of(Processo.builder().id(9L).build()));
        when(currentUserService.getRequired()).thenReturn(Usuario.builder().id(5L).build());
        when(operacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        SisbajudBloqueioService service = new SisbajudBloqueioService(processoRepository, operacaoRepository, (cpf, valor, oficio) -> { throw new RuntimeException("bacen indisponível"); }, currentUserService, authorizationService, mock(AuditLedgerService.class), rawPolicy);
        var result = service.solicitarBloqueio(new SisbajudBloqueioRequest(9L, "123", new BigDecimal("10.00"), "OF", false), "AUTHZ-1");
        assertThat(result.success()).isFalse();
        assertThat(result.detail()).contains("indisponível");
        verify(rawPolicy).markWrite();
    }
}
