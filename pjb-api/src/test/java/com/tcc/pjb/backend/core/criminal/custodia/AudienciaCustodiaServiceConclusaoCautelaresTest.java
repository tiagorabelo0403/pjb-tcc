package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.criminal.custodia.domain.ConcluirAudienciaCommand;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.AudienciaCustodia;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.AudienciaCustodiaRepository;
import com.tcc.pjb.backend.model.repository.BnmpConsultaLogRepository;
import com.tcc.pjb.backend.model.repository.MedidaCautelarRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AudienciaCustodiaServiceConclusaoCautelaresTest {

    @Test
    void shouldRegisterCautelaresAndSetLiberdadeProvisoria() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        MedidaCautelarRepository medidaCautelarRepository = mock(MedidaCautelarRepository.class);
        BnmpConsultaLogRepository bnmpConsultaLogRepository = mock(BnmpConsultaLogRepository.class);
        BnmpConsultaService bnmpConsultaService = mock(BnmpConsultaService.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        Processo processo = Processo.builder().id(50L).ramoDireito(RamoDireito.PENAL).statusProcesso(StatusProcesso.AUDIENCIA_CUSTODIA_PENDENTE).build();
        AudienciaCustodia custodia = AudienciaCustodia.builder().id(7L).processoId(50L).presoCpf("12345678901").status("PENDENTE").dataPrisao(Instant.now()).prazoLimite24h(Instant.now()).build();
        when(custodiaRepository.findById(7L)).thenReturn(Optional.of(custodia));
        when(processoRepository.findById(50L)).thenReturn(Optional.of(processo));
        when(bnmpConsultaService.consultarMandadoAtivo("12345678901")).thenReturn(new com.tcc.pjb.backend.core.criminal.custodia.domain.BnmpConsultaResult(false, null, Instant.now()));
        when(currentUserService.currentUserIdOrZero()).thenReturn(11L);
        when(custodiaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AudienciaCustodiaService service = new AudienciaCustodiaService(
                processoRepository,
                custodiaRepository,
                medidaCautelarRepository,
                bnmpConsultaLogRepository,
                bnmpConsultaService,
                currentUserService,
                rawPolicy,
                mock(AuditLedgerService.class));

        var result = service.concluirAudiencia(new ConcluirAudienciaCommand(7L, "LIBERDADE_PROVISORIA", List.of("COMPARECIMENTO_PERIODICO", "MONITORACAO_ELETRONICA")));

        assertThat(result.statusProcesso()).isEqualTo(StatusProcesso.LIBERDADE_PROVISORIA.name());
        verify(medidaCautelarRepository, times(2)).save(any());
        verify(rawPolicy).markWrite();
    }
}
