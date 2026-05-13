package com.tcc.pjb.backend.core.criminal.custodia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
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

class AudienciaCustodiaServiceStateSnapshotsTest {

    @Test
    void shouldExposeAndamentoTimelineResultadoEAuditoria() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        MedidaCautelarRepository medidaRepository = mock(MedidaCautelarRepository.class);
        AudienciaCustodia custodia = AudienciaCustodia.builder()
                .id(41L)
                .processoId(90L)
                .presoNome("José")
                .presoCpf("12345678901")
                .dataPrisao(Instant.parse("2026-04-10T10:00:00Z"))
                .prazoLimite24h(Instant.parse("2026-04-11T10:00:00Z"))
                .status("REALIZADA")
                .resultado("LIBERDADE_PROVISORIA")
                .realizadaEm(Instant.parse("2026-04-11T09:00:00Z"))
                .build();
        MedidaCautelar medida = MedidaCautelar.builder()
                .id(51L)
                .processoId(90L)
                .tipo("COMPARECIMENTO_PERIODICO")
                .ativa(true)
                .build();
        when(custodiaRepository.findById(41L)).thenReturn(Optional.of(custodia));
        when(medidaRepository.findById(51L)).thenReturn(Optional.of(medida));
        when(medidaRepository.findByProcessoIdAndAtivaTrueOrderByProximoComparecimentoAsc(90L)).thenReturn(List.of(medida));
        AudienciaCustodiaService service = new AudienciaCustodiaService(
                processoRepository,
                custodiaRepository,
                medidaRepository,
                mock(BnmpConsultaLogRepository.class),
                mock(BnmpConsultaService.class),
                mock(CurrentUserService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class));

        var andamento = service.andamento(41L);
        var timeline = service.consultarTimeline(new com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineCommand(41L));
        var resultado = service.resultadoSnapshot(41L);
        var auditoria = service.auditoria(41L);
        var medidaView = service.medidaView(51L);

        assertThat(andamento.status()).isEqualTo("REALIZADA");
        assertThat(timeline.entries()).extracting("evento").contains("PRISAO_REGISTRADA", "AUDIENCIA_REALIZADA");
        assertThat(resultado.resultado()).isEqualTo("LIBERDADE_PROVISORIA");
        assertThat(auditoria.processoId()).isEqualTo(90L);
        assertThat(medidaView.tipo()).isEqualTo("COMPARECIMENTO_PERIODICO");
    }
}
