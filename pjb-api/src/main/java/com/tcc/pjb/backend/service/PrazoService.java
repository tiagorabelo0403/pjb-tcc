package com.tcc.pjb.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calculo.PrazosEngine;
import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusEvento;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoEvento;
import com.tcc.pjb.backend.model.repository.EventoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class PrazoService {

    private final EventoProcessualRepository eventoProcessualRepository;
    private final ProcessoRepository processoRepository;
    private final PrazosEngine prazosEngine;

    public PrazoService(
            EventoProcessualRepository eventoProcessualRepository,
            ProcessoRepository processoRepository,
            PrazosEngine prazosEngine
    ) {
        this.eventoProcessualRepository = eventoProcessualRepository;
        this.processoRepository = processoRepository;
        this.prazosEngine = prazosEngine;
    }

    @Transactional
    public EventoProcessual criarPrazoLegalAutomatico(Long processoId, StatusProcesso novoStatus) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + processoId));

        Usuario responsavel = processo.getUsuario();
        int diasCorridos;
        String titulo = "";

        switch (novoStatus) {
            case CITACAO_REALIZADA:
                diasCorridos = 15;
                titulo = "Prazo Legal: Apresentar Contestação";
                break;
            case SENTENCA_PROFERIDA:
                diasCorridos = 15;
                titulo = "Prazo Legal: Interpor Recurso de Apelação";
                break;
            case EMBARGOS_DECLARACAO:
                diasCorridos = 5;
                titulo = "Prazo Legal: Contrarrazões aos Embargos";
                break;
            default:
                diasCorridos = 0;
        }

        if (diasCorridos == 0) {
            throw new RegraNegocioException("Status de processo nao gera prazo automatico: " + novoStatus.name());
        }

        LocalDateTime dataInicio = LocalDateTime.now();
        LocalDateTime dataFim = prazosEngine.calcularDataFim(
                dataInicio,
                diasCorridos,
                PrazoRegime.UTEIS,
                processo.getJurisdicao() != null ? processo.getJurisdicao().getEstado() : null,
                processo.getJurisdicao() != null ? processo.getJurisdicao().getComarca() : null
        );

        EventoProcessual evento = new EventoProcessual();
        evento.setProcesso(processo);
        evento.setResponsavel(responsavel);
        evento.setTipo(TipoEvento.PRAZO_LEGAL);
        evento.setTitulo(titulo);
        evento.setDescricao("Prazo processual calculado automaticamente pelo PJB.");
        evento.setDataInicio(dataInicio);
        evento.setDataFim(dataFim);

        return eventoProcessualRepository.save(evento);
    }

    @Transactional
    public EventoProcessual agendarAudiencia(Long processoId, Long juizId, LocalDateTime data) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + processoId));

        boolean conflito = verificarConflitoAgenda(processo.getUsuario().getId(), data);
        if (conflito) {
            throw new RegraNegocioException("Conflito de agenda detectado para o advogado responsavel.");
        }

        EventoProcessual evento = new EventoProcessual();
        evento.setProcesso(processo);
        evento.setResponsavel(processo.getUsuario());
        evento.setTipo(TipoEvento.AUDIENCIA_CONCILIACAO);
        evento.setTitulo("Audiência de Conciliação - " + processo.getNumeroUnificado());
        evento.setDataInicio(data);
        evento.setDataFim(data.plusHours(1));

        return eventoProcessualRepository.save(evento);
    }

    @Transactional(readOnly = true)
    public List<EventoProcessual> getAgendaDoUsuario(Long usuarioId) {
        return eventoProcessualRepository.findByResponsavel_IdAndStatusOrderByDataFimAsc(usuarioId, StatusEvento.PENDENTE);
    }


    private boolean verificarConflitoAgenda(Long usuarioId, LocalDateTime data) {
        List<EventoProcessual> eventos = eventoProcessualRepository.findEventosNaAgenda(usuarioId, data.minusMinutes(59), data.plusMinutes(59));
        return !eventos.isEmpty();
    }
}