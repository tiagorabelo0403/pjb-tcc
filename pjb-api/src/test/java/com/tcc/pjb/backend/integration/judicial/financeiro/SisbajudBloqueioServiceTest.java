package com.tcc.pjb.backend.integration.judicial.financeiro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SisbajudOperacaoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioRequest;

class SisbajudBloqueioServiceTest {

    @Test
    void deveRegistrarBloqueioConfirmado() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        SisbajudOperacaoRepository operacaoRepository = mock(SisbajudOperacaoRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        when(processoRepository.findById(9L)).thenReturn(Optional.of(Processo.builder().id(9L).build()));
        when(currentUserService.getRequired()).thenReturn(Usuario.builder().id(5L).build());
        when(operacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SisbajudBloqueioService service = new SisbajudBloqueioService(
                processoRepository,
                operacaoRepository,
                (cpf, valor, oficio) -> new com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudHttpResponse("PROTO-1", "ok"),
                currentUserService,
                authorizationService,
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5))
        );
        var result = service.solicitarBloqueio(
                new SisbajudBloqueioRequest(9L, "12345678901", new BigDecimal("150.00"), "OF-1", false),
                "AUTHZ-1"
        );
        assertThat(result.success()).isTrue();
        assertThat(result.protocolo()).isEqualTo("PROTO-1");
    }
}
