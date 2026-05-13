package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intelligence.StructuredProcessSummaryResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeSentencaDraft;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeSentencaDraftRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeSentencaStatus;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StructuredProcessSummaryService {

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final WorkItemRepository workItemRepository;
    private final LaianeSentencaDraftRepository laianeSentencaDraftRepository;
    private final PjbAuthorizationService authorizationService;

    public StructuredProcessSummaryService(ProcessoRepository processoRepository,
                                           MovimentacaoProcessualRepository movimentacaoRepository,
                                           WorkItemRepository workItemRepository,
                                           LaianeSentencaDraftRepository laianeSentencaDraftRepository,
                                           PjbAuthorizationService authorizationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.laianeSentencaDraftRepository = Objects.requireNonNull(laianeSentencaDraftRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Transactional(readOnly = true)
    public StructuredProcessSummaryResponse summarize(Long processoId) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return summarize(processo);
    }

    @Transactional(readOnly = true)
    public StructuredProcessSummaryResponse summarize(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        List<MovimentacaoProcessual> movimentos = movimentacaoRepository.findTop80ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId());
        LaianeSentencaDraft latestDraft = laianeSentencaDraftRepository
                .findFirstByProcesso_IdAndStatusInOrderByCreatedAtDesc(processo.getId(), List.of(LaianeSentencaStatus.DRAFT, LaianeSentencaStatus.PUBLISHED))
                .orElse(null);
        ArrayList<String> partes = new ArrayList<>();
        if (processo.getParteAutoraNome() != null && !processo.getParteAutoraNome().isBlank()) {
            partes.add("Autor(a): " + processo.getParteAutoraNome().trim());
        }
        if (processo.getParteReuNome() != null && !processo.getParteReuNome().isBlank()) {
            partes.add("Réu/Ré: " + processo.getParteReuNome().trim());
        }
        ArrayList<String> pedidos = splitCandidates(processo.getPedidosConsolidados(), processo.getPedidoPrincipal(), 6, 180);
        ArrayList<String> provas = splitCandidates(processo.getMaterialProbatorioResumo(), processo.getMaterialProbatorioHash(), 5, 180);
        ArrayList<String> decisoes = new ArrayList<>();
        movimentos.stream()
                .filter(Objects::nonNull)
                .map(MovimentacaoProcessual::getDescricao)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(this::isDecisionLike)
                .limit(5)
                .forEach(decisoes::add);
        if (latestDraft != null && latestDraft.getDraftMarkdown() != null && !latestDraft.getDraftMarkdown().isBlank()) {
            decisoes.add("Minuta judicial existente: " + compact(latestDraft.getDraftMarkdown(), 180));
        }
        ArrayList<String> estadoAtual = new ArrayList<>();
        estadoAtual.add("Fase atual: " + (processo.getFaseAtual() != null ? processo.getFaseAtual().name() : "NÃO INFORMADA"));
        estadoAtual.add("Status: " + (processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : "NÃO INFORMADO"));
        if (processo.getRito() != null) {
            estadoAtual.add("Rito: " + processo.getRito().name());
        }
        if (processo.getResumoIA() != null && !processo.getResumoIA().isBlank()) {
            estadoAtual.add("Síntese atual: " + compact(processo.getResumoIA(), 220));
        }
        ArrayList<String> proximosAtos = new ArrayList<>();
        Instant dueAt = workItemRepository.minOpenDueAtForProcesso(processo.getId());
        if (dueAt != null) {
            long days = Math.max(0L, ChronoUnit.DAYS.between(Instant.now(), dueAt));
            proximosAtos.add("Próximo marco operacional aberto em aproximadamente " + days + " dia(s).");
        }
        long openItems = workItemRepository.countOpenByProcesso(processo.getId());
        if (openItems > 0) {
            proximosAtos.add("Há " + openItems + " work item(ns) abertos aguardando providência.");
        }
        if (proximosAtos.isEmpty()) {
            proximosAtos.add("Sem pendência operacional crítica aberta na fila atual.");
        }
        ArrayList<String> alertas = new ArrayList<>();
        if (processo.getNivelSigilo() != null) {
            alertas.add("Sigilo: " + processo.getNivelSigilo().name());
        }
        if (processo.getScoreComplexidade() != null && processo.getScoreComplexidade() >= 70) {
            alertas.add("Complexidade estrutural elevada, recomendando revisão com checklist de mérito e prova.");
        }
        ArrayList<String> fontes = new ArrayList<>();
        fontes.add("Cadastro processual consolidado");
        fontes.add("Movimentações processuais recentes");
        if (latestDraft != null) {
            fontes.add("Minuta judicial assistida mais recente");
        }
        String quickSummary = buildQuickSummary(processo, pedidos, provas, decisoes, proximosAtos);
        return new StructuredProcessSummaryResponse(
                processo.getId(),
                firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())),
                quickSummary,
                120,
                List.copyOf(dedup(partes)),
                List.copyOf(dedup(pedidos)),
                List.copyOf(dedup(provas)),
                List.copyOf(dedup(decisoes)),
                List.copyOf(dedup(estadoAtual)),
                List.copyOf(dedup(proximosAtos)),
                List.copyOf(dedup(alertas)),
                List.copyOf(dedup(fontes))
        );
    }

    private boolean isDecisionLike(String value) {
        String upper = value.toUpperCase();
        return upper.contains("DECIS") || upper.contains("SENTEN") || upper.contains("DESPACH") || upper.contains("ACORD") || upper.contains("HOMOLOG");
    }

    private ArrayList<String> splitCandidates(String primary, String fallback, int limit, int maxChars) {
        ArrayList<String> out = new ArrayList<>();
        for (String source : List.of(primary, fallback)) {
            if (source == null || source.isBlank()) {
                continue;
            }
            for (String piece : source.replace('\r', '\n').split("(?:\\n|;|\\|)")) {
                if (piece == null) {
                    continue;
                }
                String trimmed = piece.trim();
                if (!trimmed.isBlank()) {
                    out.add(compact(trimmed, maxChars));
                }
                if (out.size() >= limit) {
                    return out;
                }
            }
        }
        return out;
    }

    private List<String> dedup(List<String> items) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (items != null) {
            for (String item : items) {
                if (item != null && !item.isBlank()) {
                    out.add(item.trim());
                }
            }
        }
        return new ArrayList<>(out);
    }

    private String buildQuickSummary(Processo processo,
                                     List<String> pedidos,
                                     List<String> provas,
                                     List<String> decisoes,
                                     List<String> proximosAtos) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Processo " + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())));
        if (processo.getClasseProcessual() != null && !processo.getClasseProcessual().isBlank()) {
            parts.add("classe " + processo.getClasseProcessual().trim());
        }
        if (!pedidos.isEmpty()) {
            parts.add("pedidos centrais: " + String.join("; ", pedidos.stream().limit(2).toList()));
        }
        if (!provas.isEmpty()) {
            parts.add("provas-chave: " + String.join("; ", provas.stream().limit(2).toList()));
        }
        if (!decisoes.isEmpty()) {
            parts.add("decisões/atos relevantes: " + String.join("; ", decisoes.stream().limit(2).toList()));
        }
        if (!proximosAtos.isEmpty()) {
            parts.add("próximo eixo: " + proximosAtos.getFirst());
        }
        return String.join(" | ", parts);
    }

    private String compact(String value, int max) {
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
