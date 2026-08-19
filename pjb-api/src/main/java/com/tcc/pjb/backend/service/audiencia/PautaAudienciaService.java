package com.tcc.pjb.backend.service.audiencia;

import com.tcc.pjb.backend.model.dto.audiencia.AudienciaResponse;
import com.tcc.pjb.backend.model.dto.audiencia.DesignarAudienciaRequest;
import com.tcc.pjb.backend.model.dto.audiencia.ReagendarAudienciaRequest;
import com.tcc.pjb.backend.model.dto.audiencia.RealizarAudienciaRequest;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusAudiencia;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PautaAudienciaService {

    private final AudienciaRepository audienciaRepository;
    private final ProcessoRepository processoRepository;

    public PautaAudienciaService(AudienciaRepository audienciaRepository,
                                  ProcessoRepository processoRepository) {
        this.audienciaRepository = Objects.requireNonNull(audienciaRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    @Transactional
    public AudienciaResponse designar(DesignarAudienciaRequest request) {
        Processo processo = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + request.processoId()));
        verificarConflitoAgenda(processo.getVara(), request.dataHora(), request.duracaoMinutos(), null);
        LocalDateTime agora = LocalDateTime.now();
        Audiencia audiencia = Audiencia.builder()
                .processo(processo)
                .tipo(request.tipo())
                .modalidade(request.modalidade())
                .status(StatusAudiencia.AGENDADA)
                .dataHora(request.dataHora())
                .duracaoMin(request.duracaoMinutos() != null ? request.duracaoMinutos() : 30)
                .local(request.local())
                .linkVideo(request.linkVideo())
                .pauta(request.pauta())
                .criadoPor(null)
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build();
        return AudienciaResponse.de(audienciaRepository.save(audiencia));
    }

    @Transactional
    public Optional<AudienciaResponse> reagendar(Long audienciaId, ReagendarAudienciaRequest request) {
        return audienciaRepository.findById(audienciaId).map(a -> {
            if (a.getStatus() == StatusAudiencia.CANCELADA || a.getStatus() == StatusAudiencia.ENCERRADA) {
                throw new IllegalStateException("Audiência já encerrada ou cancelada");
            }
            String vara = a.getProcesso() == null ? null : a.getProcesso().getVara();
            verificarConflitoAgenda(vara, request.novaDataHora(), a.getDuracaoMin(), a.getId());
            a.setDataHora(request.novaDataHora());
            a.setStatus(StatusAudiencia.REDESIGNADA);
            a.setNotas(request.motivo());
            a.setAtualizadoEm(LocalDateTime.now());
            return AudienciaResponse.de(audienciaRepository.save(a));
        });
    }

    @Transactional
    public Optional<AudienciaResponse> cancelar(Long audienciaId, String motivo) {
        return audienciaRepository.findById(audienciaId).map(a -> {
            if (a.getStatus() == StatusAudiencia.CANCELADA || a.getStatus() == StatusAudiencia.ENCERRADA) {
                throw new IllegalStateException("Audiência já encerrada");
            }
            a.setStatus(StatusAudiencia.CANCELADA);
            a.setNotas(motivo);
            a.setAtualizadoEm(LocalDateTime.now());
            return AudienciaResponse.de(audienciaRepository.save(a));
        });
    }

    @Transactional
    public Optional<AudienciaResponse> realizar(Long audienciaId, RealizarAudienciaRequest request) {
        return audienciaRepository.findById(audienciaId).map(a -> {
            a.setStatus(request.resultadoStatus());
            a.setNotas(request.notas());
            a.setAtualizadoEm(LocalDateTime.now());
            return AudienciaResponse.de(audienciaRepository.save(a));
        });
    }

    @Transactional(readOnly = true)
    public Optional<AudienciaResponse> buscarPorId(Long audienciaId) {
        return audienciaRepository.findById(audienciaId).map(AudienciaResponse::de);
    }

    @Transactional(readOnly = true)
    public List<AudienciaResponse> listarAgendaPorVara(String vara, LocalDate inicio, LocalDate fim) {
        LocalDateTime from = inicio.atStartOfDay();
        LocalDateTime to = fim.atTime(LocalTime.MAX);
        return audienciaRepository.findAgendaPorVara(vara, from, to)
                .stream()
                .map(AudienciaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AudienciaResponse> listarPorProcesso(Long processoId) {
        return audienciaRepository.findByProcessoIdOrderByDataHoraDesc(processoId)
                .stream()
                .map(AudienciaResponse::de)
                .toList();
    }

    private void verificarConflitoAgenda(String vara, LocalDateTime dataHora, Integer duracaoMinutos, Long excluirAudienciaId) {
        if (vara == null || vara.isBlank() || dataHora == null) {
            return;
        }
        int duracao = duracaoMinutos != null ? duracaoMinutos : 30;
        LocalDateTime fimNova = dataHora.plusMinutes(duracao);
        LocalDateTime inicioJanela = dataHora.toLocalDate().atStartOfDay();
        LocalDateTime fimJanela = dataHora.toLocalDate().atTime(LocalTime.MAX);
        for (Audiencia existente : audienciaRepository.findAgendaPorVara(vara, inicioJanela, fimJanela)) {
            if (excluirAudienciaId != null && excluirAudienciaId.equals(existente.getId())) {
                continue;
            }
            int duracaoExistente = existente.getDuracaoMin() != null ? existente.getDuracaoMin() : 30;
            LocalDateTime fimExistente = existente.getDataHora().plusMinutes(duracaoExistente);
            boolean sobrepoe = dataHora.isBefore(fimExistente) && existente.getDataHora().isBefore(fimNova);
            if (sobrepoe) {
                throw new IllegalStateException("Conflito de agenda: já existe audiência marcada na vara "
                        + vara + " entre " + existente.getDataHora() + " e " + fimExistente + ".");
            }
        }
    }
}
