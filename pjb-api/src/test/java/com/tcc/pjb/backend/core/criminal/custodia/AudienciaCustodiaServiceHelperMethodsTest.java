package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineCommand;
import com.tcc.pjb.backend.model.entity.criminal.AudienciaCustodia;
import com.tcc.pjb.backend.model.entity.criminal.MedidaCautelar;
import com.tcc.pjb.backend.model.repository.AudienciaCustodiaRepository;
import com.tcc.pjb.backend.model.repository.BnmpConsultaLogRepository;
import com.tcc.pjb.backend.model.repository.MedidaCautelarRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AudienciaCustodiaServiceHelperMethodsTest {

    @Test
    void shouldExposeTimelineAndHelperSnapshots() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        MedidaCautelarRepository medidaRepository = mock(MedidaCautelarRepository.class);
        BnmpConsultaService bnmpConsultaService = mock(BnmpConsultaService.class);
        BnmpConsultaLogRepository logRepository = mock(BnmpConsultaLogRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);

        AudienciaCustodia custodia = AudienciaCustodia.builder()
                .id(81L)
                .processoId(91L)
                .presoNome("José")
                .presoCpf("111")
                .dataPrisao(Instant.parse("2026-04-11T10:00:00Z"))
                .prazoLimite24h(Instant.parse("2026-04-12T10:00:00Z"))
                .status("REALIZADA")
                .resultado("LIBERDADE_PROVISORIA")
                .realizadaEm(Instant.parse("2026-04-11T14:00:00Z"))
                .build();
        MedidaCautelar medida = MedidaCautelar.builder()
                .id(301L)
                .processoId(91L)
                .tipo("COMPARECIMENTO")
                .descricao("comparecer")
                .ativa(true)
                .proximoComparecimento(Instant.parse("2026-04-20T10:00:00Z"))
                .build();
        when(custodiaRepository.findById(81L)).thenReturn(Optional.of(custodia));
        when(medidaRepository.findById(301L)).thenReturn(Optional.of(medida));
        when(medidaRepository.findByProcessoIdAndAtivaTrueOrderByProximoComparecimentoAsc(91L)).thenReturn(List.of(medida));

        AudienciaCustodiaService service = new AudienciaCustodiaService(
                processoRepository,
                custodiaRepository,
                medidaRepository,
                logRepository,
                bnmpConsultaService,
                currentUserService,
                rawPolicy,
                auditLedger);

        var timeline = service.consultarTimeline(new CustodiaConsultaTimelineCommand(81L));
        var andamento = service.andamento(81L);
        var medidaSnapshot = service.medidaSnapshot(301L);
        var medidaView = service.medidaView(301L);
        var resultado = service.resultadoSnapshot(81L);
        var auditoria = service.auditoria(81L);
        var medidas = service.medidas(91L);

        assertThat(timeline.entries()).hasSize(3);
        assertThat(andamento.status()).isEqualTo("REALIZADA");
        assertThat(medidaSnapshot.tipo()).isEqualTo("COMPARECIMENTO");
        assertThat(medidaView.tipo()).isEqualTo("COMPARECIMENTO");
        assertThat(resultado.resultado()).isEqualTo("LIBERDADE_PROVISORIA");
        assertThat(auditoria.processoId()).isEqualTo(91L);
        assertThat(medidas).hasSize(1);
    }
}
