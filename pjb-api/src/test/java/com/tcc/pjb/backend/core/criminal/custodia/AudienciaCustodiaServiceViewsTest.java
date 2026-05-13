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

class AudienciaCustodiaServiceViewsTest {

    @Test
    void shouldExposeTimelineMeasureViewResultAndAudit() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        AudienciaCustodiaRepository custodiaRepository = mock(AudienciaCustodiaRepository.class);
        MedidaCautelarRepository medidaRepository = mock(MedidaCautelarRepository.class);
        AudienciaCustodia custodia = AudienciaCustodia.builder()
                .id(41L)
                .processoId(11L)
                .presoNome("Beltrano")
                .dataPrisao(Instant.parse("2026-04-11T08:00:00Z"))
                .prazoLimite24h(Instant.parse("2026-04-12T08:00:00Z"))
                .status("REALIZADA")
                .resultado("LIBERDADE_PROVISORIA")
                .realizadaEm(Instant.parse("2026-04-11T12:00:00Z"))
                .createdAt(Instant.now())
                .build();
        MedidaCautelar medida = MedidaCautelar.builder()
                .id(5L)
                .processoId(11L)
                .tipo("COMPARECIMENTO_PERIODICO")
                .ativa(true)
                .proximoComparecimento(Instant.parse("2026-04-20T12:00:00Z"))
                .createdAt(Instant.now())
                .build();
        when(custodiaRepository.findById(41L)).thenReturn(Optional.of(custodia));
        when(medidaRepository.findById(5L)).thenReturn(Optional.of(medida));
        when(medidaRepository.findByProcessoIdAndAtivaTrueOrderByProximoComparecimentoAsc(11L)).thenReturn(List.of(medida));

        AudienciaCustodiaService service = new AudienciaCustodiaService(
                processoRepository,
                custodiaRepository,
                medidaRepository,
                mock(BnmpConsultaLogRepository.class),
                mock(BnmpConsultaService.class),
                mock(CurrentUserService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                mock(AuditLedgerService.class));

        var timeline = service.consultarTimeline(new com.tcc.pjb.backend.core.criminal.custodia.domain.CustodiaConsultaTimelineCommand(41L));
        var medidaView = service.medidaView(5L);
        var resultado = service.resultadoSnapshot(41L);
        var auditoria = service.auditoria(41L);
        var medidas = service.medidas(11L);

        assertThat(timeline.entries()).hasSize(3);
        assertThat(medidaView.tipo()).isEqualTo("COMPARECIMENTO_PERIODICO");
        assertThat(resultado.resultado()).isEqualTo("LIBERDADE_PROVISORIA");
        assertThat(auditoria.processoId()).isEqualTo(11L);
        assertThat(medidas).hasSize(1);
    }
}
