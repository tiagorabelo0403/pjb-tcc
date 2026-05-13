package com.tcc.pjb.backend.core.processo.timeline.application;

import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalDecisionCarryOverAssembler;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineIdentity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoTimelineApplicationService {

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final DocumentoPaginaRepository documentoPaginaRepository;

    public ProcessoTimelineApplicationService(ProcessoRepository processoRepository,
                                              DocumentoProcessualRepository documentoProcessualRepository,
                                              DocumentoPaginaRepository documentoPaginaRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.documentoPaginaRepository = Objects.requireNonNull(documentoPaginaRepository);
    }

    @Transactional(readOnly = true)
    public ProcessoTimelineAggregate detalhar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ArrayList<String> alertas = new ArrayList<>();
        ArrayList<String> proximoCiclo = new ArrayList<>();
        ArrayList<com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineEvento> eventos = new ArrayList<>();
        ArrayList<com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelinePendencia> pendencias = new ArrayList<>();
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            alertas.add("O processo exige credencial reforçada para determinados atos e documentos.");
            pendencias.add(new com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelinePendencia(
                    "SIGILO_CREDENCIAL",
                    "Credencial reforçada pendente",
                    "SIGILO",
                    "ALTA",
                    Instant.now(),
                    "SEGURANCA",
                    true,
                    List.of("Credencial reforçada exigida para continuidade segura.")
            ));
        }
        if (processo.getMaterialProbatorioResumo() != null && !processo.getMaterialProbatorioResumo().isBlank()) {
            alertas.add("Há material probatório relevante aguardando consumo operacional.");
            proximoCiclo.add("CONSUMIR_MATERIAL_PROBATORIO");
            eventos.add(new com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineEvento(
                    "MATERIAL_PROBATORIO",
                    "Material probatório aguardando consumo",
                    "PROVA",
                    2L,
                    Instant.now(),
                    false,
                    false,
                    "SECRETARIA",
                    List.of(processo.getMaterialProbatorioResumo()),
                    List.of("Há insumo probatório a ser processado.")
            ));
        }
        if (processo.getFaseAtual() != null) {
            proximoCiclo.add("AVANCAR_FASE_" + processo.getFaseAtual().name());
            eventos.add(new com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineEvento(
                    "FASE_ATUAL",
                    "Fase processual atual",
                    processo.getFaseAtual().name(),
                    1L,
                    processo.getDataUltimaMovimentacao() == null ? Instant.now() : processo.getDataUltimaMovimentacao().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                    true,
                    false,
                    "SISTEMA",
                    List.of(processo.getFaseAtual().name()),
                    List.of("Estado processual materializado.")
            ));
        }
        if ((processo.getStatusProcesso() != null && processo.getStatusProcesso().isPosDecisao()) || processo.getFaseAtual() == com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual.RECURSAL) {
            var caderno = ProcessoRecursalDecisionCarryOverAssembler.assemble(
                    processo,
                    processo.getStatusProcesso() != null && processo.getStatusProcesso().isRecursalOuEmbargos()
                            ? processo.getStatusProcesso() == com.tcc.pjb.backend.model.entity.enums.StatusProcesso.EMBARGOS_DECLARACAO
                            ? "EMBARGOS_MESMO_GRAU"
                            : "RECURSO_GRAU_SUPERIOR"
                            : "DECISAO_ANTERIOR_VINCULADA",
                    processo.getStatusProcesso() != null && processo.getStatusProcesso().isRecursalOuEmbargos()
                            ? processo.getStatusProcesso() == com.tcc.pjb.backend.model.entity.enums.StatusProcesso.EMBARGOS_DECLARACAO
                            ? "TRAMITACAO_ATIVA_NO_MESMO_GRAU"
                            : "SOMENTE_REMESSA_E_RETORNO_NO_GRAU_REMETENTE"
                            : "TRILHA_DECISORIA_ORIGEM",
                    processo.getStatusProcesso() != null && processo.getStatusProcesso().isRecursalOuEmbargos()
                            ? processo.getStatusProcesso() == com.tcc.pjb.backend.model.entity.enums.StatusProcesso.EMBARGOS_DECLARACAO
                            ? "TRAMITACAO_ATIVA_NO_MESMO_GRAU"
                            : "TRAMITACAO_ATIVA_NO_GRAU_DESTINO"
                            : "TRILHA_DECISORIA_ORIGEM",
                    documentoProcessualRepository,
                    documentoPaginaRepository
            );
            if (caderno.available()) {
                alertas.add("O caderno decisório recursal preserva a decisão original íntegra para consulta completa do julgador competente.");
                eventos.add(new com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineEvento(
                        "CADERNO_DECISORIO_RECURSAL",
                        "Decisão original carregada para recurso ou embargos",
                        "DECISAO_VINCULADA",
                        3L,
                        processo.getDataUltimaMovimentacao() == null ? Instant.now() : processo.getDataUltimaMovimentacao().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                        true,
                        false,
                        "SISTEMA",
                        ProcessoRecursalDecisionCarryOverAssembler.toSurfaceLines(caderno),
                        caderno.fundamentosExibicao()
                ));
            }
        }
        if (proximoCiclo.isEmpty()) {
            proximoCiclo.add("MANTER_TRILHA_ATUAL");
        }
        if (processo.getDataCriacao() != null && processo.getDataCriacao().isBefore(java.time.LocalDateTime.now().minusDays(15))) {
            pendencias.add(new com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelinePendencia(
                    "PRAZO_ESTRUTURAL",
                    "Prazo estrutural requer revisão",
                    "PRAZO",
                    "ALTA",
                    processo.getDataCriacao().plusDays(15).atZone(java.time.ZoneId.systemDefault()).toInstant(),
                    "SECRETARIA",
                    true,
                    List.of("Linha do tempo detectou janela estrutural madura sem fechamento equivalente.")
            ));
            alertas.add("Existe janela estrutural madura exigindo destravamento do próximo passo.");
        }
        long bloqueios = pendencias.stream().filter(com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelinePendencia::bloqueiaProximoPasso).count();
        ArrayList<String> eixosAtivos = new ArrayList<>();
        if (processo.getFaseAtual() != null) {
            eixosAtivos.add(processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            eixosAtivos.add(processo.getStatusProcesso().name());
        }
        if (processo.getRamoDireito() != null) {
            eixosAtivos.add(processo.getRamoDireito().name());
        }
        return new ProcessoTimelineAggregate(
                new ProcessoTimelineIdentity(
                        processo.getId(),
                        processo.getNumeroProcesso() != null ? processo.getNumeroProcesso() : processo.getNumero(),
                        processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                        processo.getRito() == null ? null : processo.getRito().name(),
                        processo.getFaseAtual() == null ? null : processo.getFaseAtual().name(),
                        processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name(),
                        processo.getTribunal() != null ? processo.getTribunal() : processo.getTribunalCodigoRoteado(),
                        processo.getVara() != null ? processo.getVara() : processo.getUnidadeJudiciariaCodigo(),
                        List.copyOf(eixosAtivos)
                ),
                eventos.size(),
                pendencias.size(),
                bloqueios,
                List.copyOf(eixosAtivos),
                List.copyOf(eventos),
                List.copyOf(pendencias),
                List.copyOf(proximoCiclo),
                List.copyOf(alertas),
                Instant.now()
        );
    }
}
