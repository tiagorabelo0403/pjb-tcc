package com.tcc.pjb.backend.service.audiencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.audiencia.DesignarAudienciaRequest;
import com.tcc.pjb.backend.model.dto.audiencia.ReagendarAudienciaRequest;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.ModalidadeAudiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PautaAudienciaServiceConflitoAgendaTest {

    private final AudienciaRepository audienciaRepository = mock(AudienciaRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PautaAudienciaService service = new PautaAudienciaService(audienciaRepository, processoRepository);

    @Test
    void rejeitaDesignarQuandoHorarioSobrepoeAudienciaJaMarcadaNaMesmaVara() {
        Processo processo = new Processo();
        processo.setId(1L);
        processo.setVara("1ª Vara Cível");
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

        LocalDateTime dataHora = LocalDateTime.of(2027, 3, 10, 14, 0);
        Audiencia existente = Audiencia.builder()
                .id(90L)
                .dataHora(dataHora.minusMinutes(15))
                .duracaoMin(30)
                .status(StatusAudiencia.AGENDADA)
                .build();
        when(audienciaRepository.findAgendaPorVara(
                "1ª Vara Cível", dataHora.toLocalDate().atStartOfDay(), dataHora.toLocalDate().atTime(LocalTime.MAX)))
                .thenReturn(List.of(existente));

        DesignarAudienciaRequest request = new DesignarAudienciaRequest(
                1L, TipoAudiencia.CONCILIACAO, ModalidadeAudiencia.CONCILIACAO, dataHora, 30, null, null, null, "juiz-teste");

        assertThatThrownBy(() -> service.designar(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflito de agenda");
    }

    @Test
    void permiteDesignarQuandoNaoHaSobreposicaoNaMesmaVara() {
        Processo processo = new Processo();
        processo.setId(2L);
        processo.setVara("2ª Vara Cível");
        when(processoRepository.findById(2L)).thenReturn(Optional.of(processo));

        LocalDateTime dataHora = LocalDateTime.of(2027, 3, 10, 14, 0);
        Audiencia existente = Audiencia.builder()
                .id(91L)
                .dataHora(dataHora.plusHours(2))
                .duracaoMin(30)
                .status(StatusAudiencia.AGENDADA)
                .build();
        when(audienciaRepository.findAgendaPorVara(
                "2ª Vara Cível", dataHora.toLocalDate().atStartOfDay(), dataHora.toLocalDate().atTime(LocalTime.MAX)))
                .thenReturn(List.of(existente));
        when(audienciaRepository.save(org.mockito.ArgumentMatchers.any(Audiencia.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DesignarAudienciaRequest request = new DesignarAudienciaRequest(
                2L, TipoAudiencia.INSTRUCAO, ModalidadeAudiencia.CONCILIACAO, dataHora, 30, null, null, null, "juiz-teste");

        assertThat(service.designar(request)).isNotNull();
    }

    @Test
    void rejeitaReagendarQuandoNovoHorarioSobrepoeOutraAudienciaExcluindoAPropria() {
        Processo processo = new Processo();
        processo.setId(3L);
        processo.setVara("Vara Única");
        Audiencia alvo = Audiencia.builder()
                .id(50L)
                .processo(processo)
                .dataHora(LocalDate.of(2027, 4, 1).atTime(9, 0))
                .duracaoMin(30)
                .status(StatusAudiencia.AGENDADA)
                .build();
        when(audienciaRepository.findById(50L)).thenReturn(Optional.of(alvo));

        LocalDateTime novaDataHora = LocalDateTime.of(2027, 4, 1, 15, 0);
        Audiencia outraAudiencia = Audiencia.builder()
                .id(51L)
                .dataHora(novaDataHora.minusMinutes(10))
                .duracaoMin(30)
                .status(StatusAudiencia.AGENDADA)
                .build();
        when(audienciaRepository.findAgendaPorVara(
                "Vara Única", novaDataHora.toLocalDate().atStartOfDay(), novaDataHora.toLocalDate().atTime(LocalTime.MAX)))
                .thenReturn(List.of(alvo, outraAudiencia));

        ReagendarAudienciaRequest request = new ReagendarAudienciaRequest(novaDataHora, "conflito de pauta");

        assertThatThrownBy(() -> service.reagendar(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conflito de agenda");
    }
}
