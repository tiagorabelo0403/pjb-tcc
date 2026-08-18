package com.tcc.pjb.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calculo.PrazosEngine;
import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoEvento;
import com.tcc.pjb.backend.model.repository.EventoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PrazoServiceTest {

    private final EventoProcessualRepository eventoRepository = mock(EventoProcessualRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PrazosEngine prazosEngine = mock(PrazosEngine.class);

    private PrazoService service;

    @BeforeEach
    void setUp() {
        service = new PrazoService(eventoRepository, processoRepository, prazosEngine);
    }

    @Test
    void deveCriarPrazoLegalParaCitacaoRealizadaCom15Dias() {
        Processo processo = processoComUsuario(10L);
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(prazosEngine.calcularDataFim(any(), eq(15), eq(PrazoRegime.UTEIS), any(), any()))
                .thenReturn(LocalDateTime.now().plusDays(20));
        EventoProcessual saved = new EventoProcessual();
        saved.setTipo(TipoEvento.PRAZO_LEGAL);
        when(eventoRepository.save(any())).thenReturn(saved);

        EventoProcessual resultado = service.criarPrazoLegalAutomatico(10L, StatusProcesso.CITACAO_REALIZADA);

        assertThat(resultado.getTipo()).isEqualTo(TipoEvento.PRAZO_LEGAL);
        ArgumentCaptor<EventoProcessual> captor = ArgumentCaptor.forClass(EventoProcessual.class);
        verify(eventoRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Prazo Legal: Apresentar Contestação");
    }

    @Test
    void deveCriarPrazoLegalParaSentencaProferidaCom15Dias() {
        Processo processo = processoComUsuario(11L);
        when(processoRepository.findById(11L)).thenReturn(Optional.of(processo));
        when(prazosEngine.calcularDataFim(any(), eq(15), eq(PrazoRegime.UTEIS), any(), any()))
                .thenReturn(LocalDateTime.now().plusDays(18));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.criarPrazoLegalAutomatico(11L, StatusProcesso.SENTENCA_PROFERIDA);

        ArgumentCaptor<EventoProcessual> captor = ArgumentCaptor.forClass(EventoProcessual.class);
        verify(eventoRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Prazo Legal: Interpor Recurso de Apelação");
    }

    @Test
    void deveCriarPrazoLegalParaEmbargosDeclaracaoCom5Dias() {
        Processo processo = processoComUsuario(12L);
        when(processoRepository.findById(12L)).thenReturn(Optional.of(processo));
        when(prazosEngine.calcularDataFim(any(), eq(5), eq(PrazoRegime.UTEIS), any(), any()))
                .thenReturn(LocalDateTime.now().plusDays(7));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.criarPrazoLegalAutomatico(12L, StatusProcesso.EMBARGOS_DECLARACAO);

        ArgumentCaptor<EventoProcessual> captor = ArgumentCaptor.forClass(EventoProcessual.class);
        verify(eventoRepository).save(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Prazo Legal: Contrarrazões aos Embargos");
    }

    @Test
    void deveLancarRegraNegocioParaStatusSemPrazoAutomatico() {
        Processo processo = processoComUsuario(13L);
        when(processoRepository.findById(13L)).thenReturn(Optional.of(processo));

        assertThatThrownBy(() -> service.criarPrazoLegalAutomatico(13L, StatusProcesso.DISTRIBUIDO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("DISTRIBUIDO");
    }

    @Test
    void deveLancarRecursoNaoEncontradoQuandoProcessoInexistente() {
        when(processoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criarPrazoLegalAutomatico(99L, StatusProcesso.CITACAO_REALIZADA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveAgendarAudienciaQuandoNaoHouverConflito() {
        Processo processo = processoComUsuario(20L);
        when(processoRepository.findById(20L)).thenReturn(Optional.of(processo));
        when(eventoRepository.findEventosNaAgenda(any(), any(), any())).thenReturn(List.of());
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime data = LocalDateTime.now().plusDays(3);
        EventoProcessual resultado = service.agendarAudiencia(20L, 1L, data);

        assertThat(resultado.getTipo()).isEqualTo(TipoEvento.AUDIENCIA_CONCILIACAO);
    }

    @Test
    void deveLancarRegraNegocioQuandoHouverConflitoDeAgenda() {
        Processo processo = processoComUsuario(21L);
        when(processoRepository.findById(21L)).thenReturn(Optional.of(processo));
        EventoProcessual conflito = new EventoProcessual();
        when(eventoRepository.findEventosNaAgenda(any(), any(), any())).thenReturn(List.of(conflito));

        LocalDateTime data = LocalDateTime.now().plusDays(1);

        assertThatThrownBy(() -> service.agendarAudiencia(21L, 1L, data))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Conflito de agenda");
    }

    private static Processo processoComUsuario(Long id) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        Processo processo = new Processo();
        processo.setId(id);
        processo.setUsuario(usuario);
        return processo;
    }
}
