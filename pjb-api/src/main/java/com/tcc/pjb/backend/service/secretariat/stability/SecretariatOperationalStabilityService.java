package com.tcc.pjb.backend.service.secretariat.stability;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalActLineService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalChecklistEngine;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalSlaService;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class SecretariatOperationalStabilityService {

    private static final Set<WorkItemStatus> ACTIVE_STATUSES = EnumSet.of(WorkItemStatus.PENDENTE, WorkItemStatus.EM_EXECUCAO);

    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueItemRepository secretariatQueueItemRepository;
    private final SecretariatQueueProjectionService projectionService;

    public SecretariatOperationalStabilityService(WorkItemRepository workItemRepository,
                                                  SecretariatQueueItemRepository secretariatQueueItemRepository,
                                                  SecretariatQueueProjectionService projectionService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.secretariatQueueItemRepository = Objects.requireNonNull(secretariatQueueItemRepository);
        this.projectionService = Objects.requireNonNull(projectionService);
    }

    @Transactional(readOnly = true)
    public GovernanceSnapshot avaliar(Processo processo,
                                      Usuario actor,
                                      SecretariatOperationalRoutingProfile routing,
                                      SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist,
                                      SecretariatOperationalSlaService.SlaSnapshot sla,
                                      SecretariatOperationalActLineService.ActLineSnapshot actLine) {
        List<WorkItem> candidates = loadSecretariatCandidates(processo.getId(), routing);
        List<WorkItemGovernanceCheck> checks = new ArrayList<>();
        List<String> fundamentos = new ArrayList<>();
        LinkedHashSet<String> recommendedActions = new LinkedHashSet<>();
        int critical = 0;
        int high = 0;
        int medium = 0;
        int stable = 0;
        for (WorkItem item : candidates) {
            WorkItemGovernanceCheck check = evaluateItem(item, processo, routing, checklist, sla, actLine);
            checks.add(check);
            switch (check.riskBand()) {
                case "CRITICA" -> critical++;
                case "ALTA" -> high++;
                case "MEDIA" -> medium++;
                default -> stable++;
            }
            recommendedActions.addAll(check.recommendedActions());
        }
        QueuePosture receipt = queuePosture(routing.receiptInboxKey(), routing.receiptQueueCode());
        QueuePosture saneamento = queuePosture(routing.saneamentoInboxKey(), routing.saneamentoQueueCode());
        QueuePosture audiencia = queuePosture(routing.audienceInboxKey(), routing.audienceQueueCode());
        QueuePosture execucao = queuePosture(routing.executionInboxKey(), routing.executionQueueCode());
        fundamentos.add("A estabilidade operacional consolida work items, projeções de fila, território, stage e sigilo na mesma malha institucional da secretaria.");
        fundamentos.add("Secretaria efetiva: " + routing.secretariatCode() + ".");
        fundamentos.add("Inbox institucional base: " + routing.receiptInboxKey() + ".");
        fundamentos.add("Candidatos avaliados: " + candidates.size() + ".");
        if (critical > 0) {
            fundamentos.add("Há desvio crítico de roteamento, território, sigilo ou projeção cartorária que pode contaminar a fila errada.");
        }
        if (high > 0 && checklist != null && !checklist.blockers().isEmpty()) {
            fundamentos.add("Bloqueios do checklist ampliam o risco operacional enquanto houver item em stage ou prazo incompatível.");
        }
        if (routing.secrecyAware()) {
            fundamentos.add("Processo em sigilo exige coesão entre fila, desk, prioridade e revisão de acesso nas projeções da secretaria.");
        }
        if (routing.conciliationPreferred()) {
            fundamentos.add("Fluxo conciliatório exige coerência entre pauta, presença e preparação de sala dentro da mesma lane da secretaria.");
        }
        if (critical == 0 && high == 0 && medium == 0) {
            recommendedActions.add("Nenhum reparo estrutural obrigatório. Monitorar apenas o SLA e o backlog da lane.");
        } else {
            recommendedActions.add("Executar estabilização da secretaria para normalizar inbox, fila, território, prioridade e projeção dos itens em aberto.");
        }
        String governanceBand = critical > 0 ? "CRITICA" : high > 0 ? "ALTA" : medium > 0 ? "MEDIA" : "ESTAVEL";
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("actorId", actor.getId());
        metrics.put("processoId", processo.getId());
        metrics.put("workItemCount", checks.size());
        metrics.put("criticalCount", critical);
        metrics.put("highCount", high);
        metrics.put("mediumCount", medium);
        metrics.put("stableCount", stable);
        metrics.put("receiptBacklog", receipt.active());
        metrics.put("saneamentoBacklog", saneamento.active());
        metrics.put("audienciaBacklog", audiencia.active());
        metrics.put("execucaoBacklog", execucao.active());
        metrics.put("blockingChecklistCount", checklist == null ? 0 : checklist.blockers().size());
        metrics.put("lateStages", sla == null ? 0 : "ATRASADO".equals(sla.band()) ? 1L : 0L);
        metrics.put("plannedActCount", actLine == null ? 0 : actLine.acts().size());
        return new GovernanceSnapshot(
                governanceBand,
                List.copyOf(checks),
                List.of(receipt, saneamento, audiencia, execucao),
                List.copyOf(fundamentos),
                List.copyOf(recommendedActions),
                Map.copyOf(metrics)
        );
    }

    @Transactional
    public StabilizationExecution estabilizar(Processo processo,
                                              Usuario actor,
                                              SecretariatOperationalRoutingProfile routing,
                                              SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist,
                                              SecretariatOperationalSlaService.SlaSnapshot sla,
                                              SecretariatOperationalActLineService.ActLineSnapshot actLine) {
        GovernanceSnapshot before = avaliar(processo, actor, routing, checklist, sla, actLine);
        List<NormalizationAction> actions = new ArrayList<>();
        Instant now = Instant.now();
        for (WorkItemGovernanceCheck check : before.items()) {
            if (!check.secretariatItem() || check.workItemStatus() == WorkItemStatus.CANCELADO || check.workItemStatus() == WorkItemStatus.CONCLUIDO) {
                continue;
            }
            WorkItem item = workItemRepository.findById(check.workItemId()).orElse(null);
            if (item == null) {
                continue;
            }
            ExpectedPlacement expected = expectedPlacement(item, processo, routing, checklist, actLine);
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            if (!Objects.equals(trimToNull(item.getInboxKey()), expected.inboxKey())) {
                item.setInboxKey(expected.inboxKey());
                normalized.add("INBOX");
            }
            if (!Objects.equals(trimToNull(item.getQueueCode()), expected.queueCode())) {
                item.setQueueCode(expected.queueCode());
                normalized.add("QUEUE");
            }
            if (!Objects.equals(trimToNull(item.getUf()), trimToNull(processo.getUf()))) {
                item.setUf(trimToNull(processo.getUf()));
                normalized.add("UF");
            }
            if (!Objects.equals(trimToNull(item.getComarca()), trimToNull(processo.getComarca()))) {
                item.setComarca(trimToNull(processo.getComarca()));
                normalized.add("COMARCA");
            }
            if (item.getAssignedRole() == null || item.getAssignedRole() != TipoUsuario.SERVIDOR_FORUM) {
                item.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
                normalized.add("ASSIGNED_ROLE");
            }
            Integer expectedPriority = expectedPriority(item, processo, routing);
            if (!Objects.equals(item.getPrioridade(), expectedPriority)) {
                item.setPrioridade(expectedPriority);
                normalized.add("PRIORIDADE");
            }
            Instant expectedDue = expectedDueAt(item, routing, now);
            if (shouldAdjustDueAt(item.getDueAt(), expectedDue)) {
                item.setDueAt(expectedDue);
                normalized.add("DUE_AT");
            }
            boolean expectedBlocking = expectedBlocking(item, processo, checklist);
            if (item.isBlocking() != expectedBlocking) {
                item.setBlocking(expectedBlocking);
                normalized.add("BLOCKING");
            }
            boolean projectionExistedBefore = secretariatQueueItemRepository.findById(item.getId()).isPresent();
            WorkItem saved = workItemRepository.save(item);
            projectionService.upsert(saved, projectionScore(saved, processo, routing, expected.stage()), projectionTags(saved, processo, routing, expected.stage(), checklist));
            normalized.add(projectionExistedBefore ? "PROJECTION_SYNCED" : "PROJECTION_CREATED");
            if (!normalized.isEmpty()) {
                actions.add(new NormalizationAction(saved.getId(), expected.stage(), List.copyOf(normalized), expected.inboxKey(), expected.queueCode()));
            }
        }
        GovernanceSnapshot after = avaliar(processo, actor, routing, checklist, sla, actLine);
        return new StabilizationExecution(before, after, List.copyOf(actions));
    }

    private List<WorkItem> loadSecretariatCandidates(Long processoId, SecretariatOperationalRoutingProfile routing) {
        List<WorkItem> all = workItemRepository.findAllByProcesso(processoId);
        List<WorkItem> out = new ArrayList<>();
        for (WorkItem item : all) {
            if (isSecretariatCandidate(item, routing)) {
                out.add(item);
            }
        }
        return List.copyOf(out);
    }

    private boolean isSecretariatCandidate(WorkItem item, SecretariatOperationalRoutingProfile routing) {
        if (item == null) {
            return false;
        }
        if (trimToNull(item.getInboxKey()) != null && trimToNull(item.getInboxKey()).startsWith("SEC:")) {
            return true;
        }
        if (Objects.equals(trimToNull(item.getQueueCode()), routing.receiptQueueCode())
                || Objects.equals(trimToNull(item.getQueueCode()), routing.saneamentoQueueCode())
                || Objects.equals(trimToNull(item.getQueueCode()), routing.audienceQueueCode())
                || Objects.equals(trimToNull(item.getQueueCode()), routing.executionQueueCode())) {
            return true;
        }
        if (item.getAssignedRole() == TipoUsuario.SERVIDOR_FORUM) {
            return true;
        }
        String template = normalize(item.getTemplateCode());
        String title = normalize(item.getTitulo());
        return template.contains("SECRETARIA")
                || template.contains("AUDIENCIA")
                || template.contains("EXPEDICAO")
                || title.contains("SECRETARIA")
                || title.contains("AUDIENCIA")
                || item.getType() == WorkItemType.AUDIENCIA
                || item.getType() == WorkItemType.INTIMACAO
                || item.getType() == WorkItemType.CITACAO
                || item.getType() == WorkItemType.CERTIDAO
                || item.getType() == WorkItemType.JUNTADA
                || item.getType() == WorkItemType.EXPEDICAO
                || item.getType() == WorkItemType.DISTRIBUICAO;
    }

    private WorkItemGovernanceCheck evaluateItem(WorkItem item,
                                                 Processo processo,
                                                 SecretariatOperationalRoutingProfile routing,
                                                 SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist,
                                                 SecretariatOperationalSlaService.SlaSnapshot sla,
                                                 SecretariatOperationalActLineService.ActLineSnapshot actLine) {
        ExpectedPlacement expected = expectedPlacement(item, processo, routing, checklist, actLine);
        SecretariatQueueItem projection = secretariatQueueItemRepository.findById(item.getId()).orElse(null);
        LinkedHashSet<String> drifts = new LinkedHashSet<>();
        LinkedHashSet<String> recommendations = new LinkedHashSet<>();
        boolean queueMatch = Objects.equals(trimToNull(item.getQueueCode()), expected.queueCode());
        boolean inboxMatch = Objects.equals(trimToNull(item.getInboxKey()), expected.inboxKey());
        boolean roleMatch = item.getAssignedRole() == TipoUsuario.SERVIDOR_FORUM;
        boolean territoryMatch = Objects.equals(trimToNull(item.getUf()), trimToNull(processo.getUf()))
                && Objects.equals(trimToNull(item.getComarca()), trimToNull(processo.getComarca()));
        boolean duePresent = item.getDueAt() != null;
        boolean projectionPresent = projection != null;
        boolean projectionInboxMatch = projectionPresent && Objects.equals(trimToNull(projection.getInboxKey()), expected.inboxKey());
        boolean projectionQueueMatch = projectionPresent && Objects.equals(trimToNull(projection.getQueueCode()), expected.queueCode());
        boolean sigiloCoherent = !routing.secrecyAware() || item.isBlocking() || projection != null && projection.isSecrecyReviewRequired();
        if (!queueMatch) {
            drifts.add("QUEUE_MISMATCH");
            recommendations.add("Realinhar queueCode do item para o stage " + expected.stage() + ".");
        }
        if (!inboxMatch) {
            drifts.add("INBOX_MISMATCH");
            recommendations.add("Reancorar o item no inbox institucional da secretaria correta.");
        }
        if (!roleMatch) {
            drifts.add("ROLE_MISMATCH");
            recommendations.add("Forçar assignedRole cartorária para evitar captura por papel errado.");
        }
        if (!territoryMatch) {
            drifts.add("TERRITORY_MISMATCH");
            recommendations.add("Normalizar UF e comarca do item conforme o processo roteado.");
        }
        if (!duePresent) {
            drifts.add("DUE_AT_MISSING");
            recommendations.add("Recalcular o prazo operacional do stage e preencher dueAt.");
        }
        if (!projectionPresent) {
            drifts.add("PROJECTION_MISSING");
            recommendations.add("Materializar ou remontar a projeção da fila da secretaria para o work item.");
        } else {
            if (!projectionInboxMatch) {
                drifts.add("PROJECTION_INBOX_MISMATCH");
                recommendations.add("Sincronizar projeção com o inbox efetivo da secretaria.");
            }
            if (!projectionQueueMatch) {
                drifts.add("PROJECTION_QUEUE_MISMATCH");
                recommendations.add("Sincronizar projeção com a fila efetiva do stage.");
            }
        }
        if (!sigiloCoherent) {
            drifts.add("SECRECY_MISMATCH");
            recommendations.add("Fortalecer o item sigiloso como blocking e com revisão de acesso na projeção.");
        }
        if (item.getStatus() != null && ACTIVE_STATUSES.contains(item.getStatus()) && item.getDueAt() != null && item.getDueAt().isBefore(Instant.now())) {
            drifts.add("OVERDUE_ACTIVE_ITEM");
            recommendations.add("Escalonar ou redistribuir o item em atraso dentro da secretaria.");
        }
        if (checklist != null && !checklist.blockers().isEmpty() && expected.blocking() && !item.isBlocking()) {
            drifts.add("CHECKLIST_BLOCKING_MISMATCH");
            recommendations.add("Propagar os blockers do checklist para o item cartorário correspondente.");
        }
        if (sla != null && expected.stage().equals(sla.stage()) && "CRITICA".equals(sla.band())) {
            drifts.add("STAGE_SLA_DELAY");
            recommendations.add("Aplicar escalonamento do stage " + expected.stage() + " pela régua de SLA da secretaria.");
        }
        String riskBand = resolveRiskBand(drifts);
        return new WorkItemGovernanceCheck(
                item.getId(),
                item.getTemplateCode(),
                item.getTitulo(),
                item.getStatus(),
                item.getType(),
                true,
                expected.stage(),
                trimToNull(item.getInboxKey()),
                trimToNull(item.getQueueCode()),
                projectionPresent,
                List.copyOf(drifts),
                List.copyOf(recommendations),
                riskBand,
                buildMetrics(item, projection, expected, queueMatch, inboxMatch, territoryMatch, roleMatch, duePresent, projectionPresent, sigiloCoherent)
        );
    }

    private Map<String, Object> buildMetrics(WorkItem item,
                                             SecretariatQueueItem projection,
                                             ExpectedPlacement expected,
                                             boolean queueMatch,
                                             boolean inboxMatch,
                                             boolean territoryMatch,
                                             boolean roleMatch,
                                             boolean duePresent,
                                             boolean projectionPresent,
                                             boolean sigiloCoherent) {
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("expectedStage", expected.stage());
        metrics.put("expectedInboxKey", expected.inboxKey());
        metrics.put("expectedQueueCode", expected.queueCode());
        metrics.put("queueMatch", queueMatch);
        metrics.put("inboxMatch", inboxMatch);
        metrics.put("territoryMatch", territoryMatch);
        metrics.put("roleMatch", roleMatch);
        metrics.put("duePresent", duePresent);
        metrics.put("projectionPresent", projectionPresent);
        metrics.put("sigiloCoherent", sigiloCoherent);
        metrics.put("currentPriority", item.getPrioridade());
        metrics.put("blocking", item.isBlocking());
        metrics.put("projectionScore", projection == null ? null : projection.getScore());
        return Map.copyOf(metrics);
    }

    private ExpectedPlacement expectedPlacement(WorkItem item,
                                                Processo processo,
                                                SecretariatOperationalRoutingProfile routing,
                                                SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist,
                                                SecretariatOperationalActLineService.ActLineSnapshot actLine) {
        String stage = detectStage(item, routing, actLine);
        String inboxKey = switch (stage) {
            case "RECEBIMENTO" -> routing.receiptInboxKey();
            case "SANEAMENTO" -> routing.saneamentoInboxKey();
            case "AUDIENCIA" -> routing.audienceInboxKey();
            default -> routing.executionInboxKey();
        };
        String queueCode = switch (stage) {
            case "RECEBIMENTO" -> routing.receiptQueueCode();
            case "SANEAMENTO" -> routing.saneamentoQueueCode();
            case "AUDIENCIA" -> routing.audienceQueueCode();
            default -> routing.executionQueueCode();
        };
        boolean blocking = expectedBlocking(item, processo, checklist);
        return new ExpectedPlacement(stage, trimToNull(inboxKey), trimToNull(queueCode), blocking);
    }

    private String detectStage(WorkItem item,
                               SecretariatOperationalRoutingProfile routing,
                               SecretariatOperationalActLineService.ActLineSnapshot actLine) {
        String queue = normalize(item.getQueueCode());
        String template = normalize(item.getTemplateCode());
        String title = normalize(item.getTitulo());
        if (equalsAny(queue, routing.receiptQueueCode()) || template.contains("RECEBIMENTO") || title.contains("RECEBIMENTO")) {
            return "RECEBIMENTO";
        }
        if (equalsAny(queue, routing.saneamentoQueueCode())
                || template.contains("SANEAMENTO")
                || template.contains("CHECKLIST")
                || template.contains("CERTIDAO")
                || title.contains("SANEAMENTO")
                || title.contains("CHECKLIST")) {
            return "SANEAMENTO";
        }
        if (equalsAny(queue, routing.audienceQueueCode())
                || item.getType() == WorkItemType.AUDIENCIA
                || template.contains("AUDIENCIA")
                || template.contains("PAUTA")
                || template.contains("PRESENCA")
                || title.contains("AUDIENCIA")) {
            return "AUDIENCIA";
        }
        if (actLine != null && actLine.acts().stream().anyMatch(act -> template.contains(normalize(act.code())) && "AUDIENCIA".equals(act.stage()))) {
            return "AUDIENCIA";
        }
        return "EXECUCAO";
    }

    private boolean equalsAny(String currentQueue, String expectedQueue) {
        return trimToNull(currentQueue) != null && Objects.equals(trimToNull(currentQueue), trimToNull(expectedQueue));
    }

    private boolean expectedBlocking(WorkItem item,
                                     Processo processo,
                                     SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            return true;
        }
        if (checklist == null || checklist.blockers().isEmpty()) {
            return item.isBlocking();
        }
        return item.isBlocking()
                || item.getType() == WorkItemType.AUDIENCIA
                || item.getType() == WorkItemType.CERTIDAO
                || item.getType() == WorkItemType.DISTRIBUICAO
                || item.getType() == WorkItemType.EXPEDICAO;
    }

    private Integer expectedPriority(WorkItem item, Processo processo, SecretariatOperationalRoutingProfile routing) {
        int priority = item.getPrioridade() == null ? 3 : item.getPrioridade();
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            priority = Math.min(priority, 1);
        }
        if (item.getType() == WorkItemType.AUDIENCIA) {
            priority = Math.min(priority, 1);
        }
        if (routing.conciliationPreferred() && item.getType() == WorkItemType.INTIMACAO) {
            priority = Math.min(priority, 2);
        }
        return Math.max(0, Math.min(priority, 5));
    }

    private Instant expectedDueAt(WorkItem item, SecretariatOperationalRoutingProfile routing, Instant now) {
        String stage = detectStage(item, routing, null);
        if (stage.equals("RECEBIMENTO")) {
            return now.plus(routing.receiptSla());
        }
        if (stage.equals("SANEAMENTO")) {
            return now.plus(routing.saneamentoSla());
        }
        if (stage.equals("AUDIENCIA")) {
            return now.plus(routing.audiencePreparationSla());
        }
        return now.plus(routing.saneamentoSla().plus(routing.receiptSla()).dividedBy(2));
    }

    private boolean shouldAdjustDueAt(Instant current, Instant expected) {
        if (current == null) {
            return true;
        }
        long delta = Math.abs(current.getEpochSecond() - expected.getEpochSecond());
        return delta > 3600L * 6;
    }

    private int projectionScore(WorkItem item, Processo processo, SecretariatOperationalRoutingProfile routing, String stage) {
        int score = 60;
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            score += 20;
        }
        if (item.isBlocking()) {
            score += 10;
        }
        if ("AUDIENCIA".equals(stage)) {
            score += 10;
        }
        if (routing.conciliationPreferred() && item.getType() == WorkItemType.AUDIENCIA) {
            score += 5;
        }
        return Math.min(score, 100);
    }

    private List<String> projectionTags(WorkItem item,
                                        Processo processo,
                                        SecretariatOperationalRoutingProfile routing,
                                        String stage,
                                        SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist) {
        List<String> tags = new ArrayList<>();
        tags.add("SECRETARIA_ESTABILIZADA");
        tags.add(stage);
        tags.add(routing.secretariatCode());
        if (processo.getRamoDireito() != null) {
            tags.add(processo.getRamoDireito().name());
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            tags.add("SIGILO");
        }
        if (item.isBlocking()) {
            tags.add("BLOCKING");
        }
        if (checklist != null && !checklist.blockers().isEmpty()) {
            tags.add("CHECKLIST_BLOCKED");
        }
        return List.copyOf(tags);
    }

    private QueuePosture queuePosture(String inboxKey, String queueCode) {
        Instant now = Instant.now();
        Object[] row = queueCode == null || queueCode.isBlank()
                ? secretariatQueueItemRepository.workload(inboxKey, ACTIVE_STATUSES.stream().map(Enum::name).toList(), now)
                : secretariatQueueItemRepository.workloadByInboxAndQueue(inboxKey, queueCode, ACTIVE_STATUSES.stream().map(Enum::name).toList(), now);
        int active = asInt(row, 0);
        int overdue = asInt(row, 1);
        int priorityPressure = asInt(row, 2);
        String band = active >= 140 || overdue >= 20 ? "SATURADA" : active >= 60 || overdue >= 8 ? "PRESSAO" : "LIVRE";
        return new QueuePosture(trimToNull(inboxKey), trimToNull(queueCode), active, overdue, priorityPressure, band);
    }

    private int asInt(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return 0;
        }
        return ((Number) row[index]).intValue();
    }

    private String resolveRiskBand(Collection<String> drifts) {
        if (drifts.contains("INBOX_MISMATCH") || drifts.contains("QUEUE_MISMATCH") || drifts.contains("SECRECY_MISMATCH") || drifts.contains("TERRITORY_MISMATCH")) {
            return "CRITICA";
        }
        if (drifts.contains("PROJECTION_MISSING") || drifts.contains("PROJECTION_QUEUE_MISMATCH") || drifts.contains("PROJECTION_INBOX_MISMATCH") || drifts.contains("OVERDUE_ACTIVE_ITEM") || drifts.contains("STAGE_SLA_DELAY")) {
            return "ALTA";
        }
        if (!drifts.isEmpty()) {
            return "MEDIA";
        }
        return "ESTAVEL";
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private String trimToNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    public record GovernanceSnapshot(
            String governanceBand,
            List<WorkItemGovernanceCheck> items,
            List<QueuePosture> queues,
            List<String> fundamentos,
            List<String> recommendedActions,
            Map<String, Object> metrics
    ) {
    }

    public record StabilizationExecution(
            GovernanceSnapshot before,
            GovernanceSnapshot after,
            List<NormalizationAction> actions
    ) {
    }

    public record WorkItemGovernanceCheck(
            Long workItemId,
            String templateCode,
            String titulo,
            WorkItemStatus workItemStatus,
            WorkItemType workItemType,
            boolean secretariatItem,
            String expectedStage,
            String currentInboxKey,
            String currentQueueCode,
            boolean projectionPresent,
            List<String> drifts,
            List<String> recommendedActions,
            String riskBand,
            Map<String, Object> metrics
    ) {
    }

    public record NormalizationAction(
            Long workItemId,
            String stage,
            List<String> normalizedFields,
            String inboxKey,
            String queueCode
    ) {
    }

    public record QueuePosture(
            String inboxKey,
            String queueCode,
            int active,
            int overdue,
            int priorityPressure,
            String workloadBand
    ) {
    }

    private record ExpectedPlacement(
            String stage,
            String inboxKey,
            String queueCode,
            boolean blocking
    ) {
    }
}
