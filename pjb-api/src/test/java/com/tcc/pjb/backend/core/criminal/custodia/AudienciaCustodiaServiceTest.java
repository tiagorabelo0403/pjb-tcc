package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.criminal.custodia.domain.ConcluirAudienciaCommand;
import com.tcc.pjb.backend.core.criminal.custodia.domain.RegistrarPrisaoCommand;

class AudienciaCustodiaServiceTest {

    @Test
    void deveRegistrarPrisaoEmProcessoPenal() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        MedidaCautelarRepository medidaCautelarRepository = mock(MedidaCautelarRepository.class);
        BnmpConsultaLogRepository bnmpConsultaLogRepository = mock(BnmpConsultaLogRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        Processo processo = Processo.builder().id(10L).ramoDireito(RamoDireito.PENAL).build();
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(custodiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserService.getOrNull()).thenReturn(new Usuario());
        AudienciaCustodiaService service = new AudienciaCustodiaService(
                processoRepository,
                custodiaRepository,
                medidaCautelarRepository,
                bnmpConsultaLogRepository,
                cpf -> com.tcc.pjb.backend.core.criminal.custodia.domain.BnmpConsultaResult.vazio(),
                currentUserService,
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class)
        );
        var result = service.registrarPrisao(new RegistrarPrisaoCommand(10L, "João", "12345678901", Instant.parse("2026-04-10T12:00:00Z")));
        assertThat(result.custodiaId()).isNull();
        assertThat(result.prazoLimite24h()).isEqualTo(Instant.parse("2026-04-11T12:00:00Z"));
        assertThat(processo.getStatusProcesso()).isNotNull();
    }

    @Test
    void deveConcluirAudienciaComLiberdadeProvisoria() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        MedidaCautelarRepository medidaCautelarRepository = mock(MedidaCautelarRepository.class);
        BnmpConsultaLogRepository bnmpConsultaLogRepository = mock(BnmpConsultaLogRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        Processo processo = Processo.builder().id(10L).ramoDireito(RamoDireito.PENAL).build();
        AudienciaCustodia custodia = AudienciaCustodia.builder().id(7L).processoId(10L).presoCpf("12345678901").status("PENDENTE").build();
        Usuario usuario = new Usuario();
        usuario.setId(55L);
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(custodiaRepository.findById(7L)).thenReturn(Optional.of(custodia));
        when(custodiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medidaCautelarRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bnmpConsultaLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserService.getOrNull()).thenReturn(usuario);
        AudienciaCustodiaService service = new AudienciaCustodiaService(
                processoRepository,
                custodiaRepository,
                medidaCautelarRepository,
                bnmpConsultaLogRepository,
                cpf -> new com.tcc.pjb.backend.core.criminal.custodia.domain.BnmpConsultaResult(false, null),
                currentUserService,
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class)
        );
        var result = service.concluirAudiencia(new ConcluirAudienciaCommand(7L, "LIBERDADE_PROVISORIA", List.of("COMPARECIMENTO_PERIODICO")));
        assertThat(result.statusProcesso()).isEqualTo("LIBERDADE_PROVISORIA");
        assertThat(result.mandadoAtivoBnmp()).isFalse();
    }
}
