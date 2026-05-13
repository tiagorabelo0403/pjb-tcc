package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.AudienciaCustodia;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.AudienciaCustodiaRepository;
import com.tcc.pjb.backend.model.repository.BnmpConsultaLogRepository;
import com.tcc.pjb.backend.model.repository.MedidaCautelarRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.criminal.custodia.domain.ConcluirAudienciaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.RegistrarPrisaoCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.BnmpConsultaResult;

class AudienciaCustodiaServiceGuardTest {

    @Test
    void shouldRejectPrisonRegistrationForNonPenalProcess() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        when(processoRepository.findById(1L)).thenReturn(Optional.of(Processo.builder().id(1L).ramoDireito(RamoDireito.CIVIL).build()));
        AudienciaCustodiaService service = new AudienciaCustodiaService(
                processoRepository,
                mock(AudienciaCustodiaRepository.class),
                mock(MedidaCautelarRepository.class),
                mock(BnmpConsultaLogRepository.class),
                cpf -> BnmpConsultaResult.vazio(),
                mock(CurrentUserService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class)
        );
        assertThatThrownBy(() -> service.registrarPrisao(new RegistrarPrisaoCommand(1L, "João", "123", Instant.now())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("penais/militares");
    }

    @Test
    void shouldSetPreventivePrisonStatusWhenResultadoIsPrison() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        Processo processo = Processo.builder().id(10L).ramoDireito(RamoDireito.PENAL).build();
        AudienciaCustodia custodia = AudienciaCustodia.builder().id(7L).processoId(10L).presoCpf("12345678901").status("PENDENTE").build();
        Usuario usuario = new Usuario(); usuario.setId(5L);
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(custodiaRepository.findById(7L)).thenReturn(Optional.of(custodia));
        when(custodiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserService.currentUserIdOrZero()).thenReturn(5L);
        AudienciaCustodiaService service = new AudienciaCustodiaService(
                processoRepository,
                custodiaRepository,
                mock(MedidaCautelarRepository.class),
                mock(BnmpConsultaLogRepository.class),
                cpf -> new BnmpConsultaResult(true, "M-1"),
                currentUserService,
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class)
        );
        service.concluirAudiencia(new ConcluirAudienciaCommand(7L, "PRISAO_PREVENTIVA", java.util.List.of()));
        org.assertj.core.api.Assertions.assertThat(processo.getStatusProcesso().name()).isEqualTo("PRESO_PROVISORIO");
    }
}
