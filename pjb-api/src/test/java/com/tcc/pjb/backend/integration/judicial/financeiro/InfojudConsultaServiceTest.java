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
import com.tcc.pjb.backend.model.repository.InfojudConsultaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InfojudConsultaServiceTest {

    @Test
    void deveRegistrarConsultaInfojudConfirmada() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        InfojudConsultaRepository consultaRepository = mock(InfojudConsultaRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
        when(processoRepository.findById(10L)).thenReturn(Optional.of(Processo.builder().id(10L).build()));
        when(currentUserService.getRequired()).thenReturn(Usuario.builder().id(7L).build());
        when(consultaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        InfojudConsultaService service = new InfojudConsultaService(
                processoRepository,
                consultaRepository,
                alvo -> new com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResponse("INFO-1", "retorno"),
                currentUserService,
                authorizationService,
                mock(AuditLedgerService.class),
                new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5))
        );
        var result = service.consultar(
                new com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaRequest(10L, "12345678901", false),
                "AUTHZ-2"
        );
        assertThat(result.success()).isTrue();
        assertThat(result.protocolo()).isEqualTo("INFO-1");
    }
}
