package com.tcc.pjb.backend.service.processual.observability.business;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.comunicacao.judicial.ExpedicaoJudicial;
import com.tcc.pjb.backend.core.comunicacao.judicial.ExpedicaoJudicialRepository;
import com.tcc.pjb.backend.model.dto.processual.observability.business.ProcessBusinessObservabilityResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.repository.CaseFileEventRepository;
import com.tcc.pjb.backend.model.repository.CaseFileRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import com.tcc.pjb.backend.service.processual.observability.metrics.ProcessBusinessMetrics;

@Service
public class ProcessBusinessObservabilityService {

    private static final Duration SNAPSHOT_CACHE_TTL = Duration.ofSeconds(20);

    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ExpedicaoJudicialRepository expedicaoJudicialRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseProceedingRepository caseProceedingRepository;
    private final CaseFileEventRepository caseFileEventRepository;
    private final ProcessBusinessMetrics metrics;
    private final AtomicReference<CachedSnapshot> snapshotCache = new AtomicReference<>();

    public ProcessBusinessObservabilityService(ProcessoRepository processoRepository,
                                               WorkItemRepository workItemRepository,
                                               OutboxEventRepository outboxEventRepository,
                                               ExpedicaoJudicialRepository expedicaoJudicialRepository,
                                               CaseFileRepository caseFileRepository,
                                               CaseProceedingRepository caseProceedingRepository,
                                               CaseFileEventRepository caseFileEventRepository,
                                               ProcessBusinessMetrics metrics) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.outboxEventRepository = Objects.requireNonNull(outboxEventRepository);
        this.expedicaoJudicialRepository = Objects.requireNonNull(expedicaoJudicialRepository);
        this.caseFileRepository = Objects.requireNonNull(caseFileRepository);
        this.caseProceedingRepository = Objects.requireNonNull(caseProceedingRepository);
        this.caseFileEventRepository = Objects.requireNonNull(caseFileEventRepository);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Transactional(readOnly = true)
    public ProcessBusinessObservabilityResponse snapshot() {
        CachedSnapshot cached = snapshotCache.get();
        if (isFresh(cached)) {
            return cached.response();
        }
        Instant reference = Instant.now();
        List<Processo> processos = processoRepository.findAll();
        List<CaseProceeding> proceedings = caseProceedingRepository.findAll();
        Map<Long, List<CaseProceeding>> proceedingsByCase = groupProceedingsByCase(proceedings);

        long total = processos.size();
        long arquivados = processos.stream().filter(p -> p.getStatusProcesso() == StatusProcesso.ARQUIVADO).count();
        long transito = processos.stream().filter(p -> p.getStatusProcesso() == StatusProcesso.TRANSITO_EM_JULGADO).count();
        long recursais = processos.stream().filter(this::isRecursal).count();
        long ativos = Math.max(0L, total - arquivados);
        long workItemsPendentes = workItemRepository.countByStatus(WorkItemStatus.PENDENTE);
        long workItemsEmExecucao = workItemRepository.countByStatus(WorkItemStatus.EM_EXECUCAO);
        long workItemsVencidos = workItemRepository.countByStatusAndDueAtBefore(WorkItemStatus.PENDENTE, reference)
                + workItemRepository.countByStatusAndDueAtBefore(WorkItemStatus.EM_EXECUCAO, reference);
        long outboxPendentes = outboxEventRepository.peekIds(com.tcc.pjb.backend.model.entity.outbox.OutboxStatus.PENDING.name(), reference, 10_000).size();
        long comunicacoesPendentes = expedicaoJudicialRepository.countByStatus(ExpedicaoJudicial.StatusExpedicao.EXPEDIDA);
        long comunicacoesFrustradas = expedicaoJudicialRepository.countByStatus(ExpedicaoJudicial.StatusExpedicao.FRUSTRADA_DEFINITIVA);
        long comunicacoesEvasao = expedicaoJudicialRepository.countByEvasaoDetectadaTrue();
        long comunicacoesEscalonadas = expedicaoJudicialRepository.countByEscalonadoParaJuizTrue();
        long caseFilesUnificados = caseFileRepository.count();
        long proceedingsMaterializados = proceedings.size();
        long proceedingsRecursais = proceedings.stream().filter(item -> item.getContinuityTrack() == CaseContinuityTrack.RECURSAL).count();
        long proceedingsExecutorios = proceedings.stream().filter(item -> item.getContinuityTrack() == CaseContinuityTrack.CUMPRIMENTO || item.getContinuityTrack() == CaseContinuityTrack.EXECUCAO).count();
        long staleProceedings = proceedings.stream().filter(item -> item.getLastSyncAt() != null && item.getLastSyncAt().isBefore(reference.minusSeconds(48 * 3600L))).count();
        long caseFileEvents = caseFileEventRepository.count();
        long orphanProceedingParents = 0L;
        long divergentRootProceedings = 0L;
        long caseFilesAttentionRequired = 0L;

        for (Map.Entry<Long, List<CaseProceeding>> entry : proceedingsByCase.entrySet()) {
            List<CaseProceeding> caseProceedings = entry.getValue();
            LinkedHashSet<String> keys = new LinkedHashSet<>();
            caseProceedings.stream().map(CaseProceeding::getProceedingKey).filter(Objects::nonNull).forEach(keys::add);
            long rootLikeCount = caseProceedings.stream().filter(item -> item.getProceedingRole() != null && item.getProceedingRole().isRootLike()).count();
            long orphanParentsForCase = caseProceedings.stream()
                    .filter(item -> item.getParentProceedingKey() != null && !item.getParentProceedingKey().isBlank())
                    .filter(item -> !keys.contains(item.getParentProceedingKey()))
                    .count();
            long incompatibleForCase = caseProceedings.stream()
                    .filter(item -> item.getProceedingRole() == null || item.getContinuityTrack() == null || !item.getProceedingRole().acceptsTrack(item.getContinuityTrack()))
                    .count();
            boolean staleCase = caseProceedings.stream().anyMatch(item -> item.getLastSyncAt() != null && item.getLastSyncAt().isBefore(reference.minusSeconds(48 * 3600L)));
            orphanProceedingParents += orphanParentsForCase;
            if (rootLikeCount != 1) {
                divergentRootProceedings++;
            }
            if (rootLikeCount != 1 || orphanParentsForCase > 0 || incompatibleForCase > 0 || staleCase) {
                caseFilesAttentionRequired++;
            }
        }

        Map<String, Long> porRamo = aggregate(processos, processo -> processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "NAO_CLASSIFICADO");
        Map<String, Long> porStatus = aggregate(processos, processo -> processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : "NAO_CLASSIFICADO");
        List<String> filasCriticas = workItemRepository.findFilasComMaisDe(25L);
        List<String> alertas = buildAlerts(ativos, recursais, workItemsVencidos, outboxPendentes, comunicacoesFrustradas, comunicacoesEvasao, filasCriticas, staleProceedings, proceedingsExecutorios, caseFilesAttentionRequired, orphanProceedingParents, divergentRootProceedings);

        ProcessBusinessObservabilityResponse response = new ProcessBusinessObservabilityResponse(
                reference,
                total,
                ativos,
                recursais,
                arquivados,
                transito,
                workItemsPendentes,
                workItemsEmExecucao,
                workItemsVencidos,
                outboxPendentes,
                comunicacoesPendentes,
                comunicacoesFrustradas,
                comunicacoesEvasao,
                comunicacoesEscalonadas,
                caseFilesUnificados,
                proceedingsMaterializados,
                proceedingsRecursais,
                proceedingsExecutorios,
                staleProceedings,
                caseFileEvents,
                caseFilesAttentionRequired,
                orphanProceedingParents,
                divergentRootProceedings,
                porRamo,
                porStatus,
                filasCriticas,
                alertas
        );
        metrics.publish(response);
        snapshotCache.set(new CachedSnapshot(response, reference.plus(SNAPSHOT_CACHE_TTL)));
        return response;
    }

    private boolean isFresh(CachedSnapshot cache) {
        return cache != null && cache.expiresAt() != null && cache.expiresAt().isAfter(Instant.now());
    }

    private boolean isRecursal(Processo processo) {
        return processo.getStatusProcesso() == StatusProcesso.RECURSO_INTERPOSTO
                || (processo.getFaseAtual() != null && processo.getFaseAtual().name().contains("RECURSAL"));
    }

    private Map<Long, List<CaseProceeding>> groupProceedingsByCase(List<CaseProceeding> proceedings) {
        LinkedHashMap<Long, List<CaseProceeding>> out = new LinkedHashMap<>();
        proceedings.stream()
                .filter(item -> item.getCaseFileId() != null)
                .forEach(item -> out.computeIfAbsent(item.getCaseFileId(), ignored -> new ArrayList<>()).add(item));
        return out;
    }

    private Map<String, Long> aggregate(List<Processo> processos, java.util.function.Function<Processo, String> classifier) {
        LinkedHashMap<String, Long> out = new LinkedHashMap<>();
        processos.stream()
                .map(classifier)
                .filter(Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .forEach(key -> out.merge(key, 1L, Long::sum));
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private List<String> buildAlerts(long ativos,
                                     long recursais,
                                     long workItemsVencidos,
                                     long outboxPendentes,
                                     long comunicacoesFrustradas,
                                     long comunicacoesEvasao,
                                     List<String> filasCriticas,
                                     long staleProceedings,
                                     long proceedingsExecutorios,
                                     long caseFilesAttentionRequired,
                                     long orphanProceedingParents,
                                     long divergentRootProceedings) {
        List<String> alertas = new ArrayList<>();
        if (workItemsVencidos > 0) {
            alertas.add("Há work items vencidos exigindo saneamento operacional imediato.");
        }
        if (outboxPendentes > 100) {
            alertas.add("A fila outbox superou o patamar de segurança operacional definido para o snapshot.");
        }
        if (comunicacoesFrustradas > 0) {
            alertas.add("Existem comunicações judiciais frustradas pendentes de reação institucional.");
        }
        if (comunicacoesEvasao > 0) {
            alertas.add("Foram detectados sinais de evasão em comunicações judiciais monitoradas.");
        }
        if (!filasCriticas.isEmpty()) {
            alertas.add("Existem filas operacionais com acúmulo acima do limiar crítico configurado.");
        }
        if (ativos > 0 && recursais * 100 >= ativos * 40) {
            alertas.add("O estoque recursal ultrapassou proporção estrutural relevante do acervo ativo.");
        }
        if (staleProceedings > 0) {
            alertas.add("Existem ramificações do caso raiz com sincronização defasada acima do limiar operacional.");
        }
        if (proceedingsExecutorios > 0 && proceedingsExecutorios * 100 >= Math.max(1L, ativos) * 30) {
            alertas.add("O volume de ramificações executórias atingiu proporção relevante do acervo ativo monitorado.");
        }
        if (caseFilesAttentionRequired > 0) {
            alertas.add("Existem casos raiz com atenção estrutural pendente no organismo unificado do processo.");
        }
        if (orphanProceedingParents > 0) {
            alertas.add("Foram detectadas ramificações com parentProceedingKey órfão no grafo do caso unificado.");
        }
        if (divergentRootProceedings > 0) {
            alertas.add("Existem casos com quantidade divergente de proceeding raiz/vínculo principal materializado.");
        }
        return List.copyOf(alertas);
    }


    private record CachedSnapshot(ProcessBusinessObservabilityResponse response, Instant expiresAt) {
    }
}
