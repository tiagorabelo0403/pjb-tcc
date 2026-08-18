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
import com.tcc.pjb.backend.model.repository.RenajudRestricaoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RenajudRestricaoServiceTest {

    @Test
    void deveRegistrarRestricaoConfirmada() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        RenajudRestricaoRepository restricaoRepository = mock(RenajudRestricaoRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        when(processoRepository.findById(11L)).thenReturn(Optional.of(Processo.builder().id(11L).build()));
        when(currentUserService.getRequired()).thenReturn(Usuario.builder().id(8L).build());
        when(restricaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RenajudRestricaoService service = new RenajudRestricaoService(
                processoRepository,
                restricaoRepository,
                (placa, renavam, tipo) -> new com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoResponse("RENA-1", "ok"),
                currentUserService,
                authorizationService,
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5))
        );
        var result = service.solicitarRestricao(
                new com.tcc.pjb.backend.integration.judicial.financeiro.domain.RenajudRestricaoRequest(11L, "RESTRICAO", "ABC1D23", "12345678901"),
                "AUTHZ-3"
        );
        assertThat(result.success()).isTrue();
        assertThat(result.protocolo()).isEqualTo("RENA-1");
    }
}
