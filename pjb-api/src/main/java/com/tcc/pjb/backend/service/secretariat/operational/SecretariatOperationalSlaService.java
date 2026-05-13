package com.tcc.pjb.backend.service.secretariat.operational;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleRuntimePolicyService;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;

@Service
public class SecretariatOperationalSlaService {

    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueProjectionService projectionService;
    private final JudicialScaleRuntimePolicyService runtimePolicyService;

    public SecretariatOperationalSlaService(WorkItemRepository workItemRepository,
                                            SecretariatQueueProjectionService projectionService,
                                            JudicialScaleRuntimePolicyService runtimePolicyService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.projectionService = Objects.requireNonNull(projectionService);
        this.runtimePolicyService = Objects.requireNonNull(runtimePolicyService);
    }

    @Transactional(readOnly = true)
    public SlaSnapshot avaliar(Processo processo,
                               SecretariatOperationalRoutingProfile routing,
                               String stage) {
        JudicialScaleRuntimePolicyService.JudicialRuntimePolicy runtimePolicy = runtimePolicyService.resolve(routing);
        StageSlaRoute stageRoute = resolveStageRoute(routing, stage, runtimePolicy);
        Instant now = Instant.now();
        List<WorkItem> openItems = workItemRepository.findAllByProcesso(processo.getId()).stream()
                .filter(item -> item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO)
                .filter(item -> matchesStage(item, stageRoute))
                .toList();
        int overdue = (int) openItems.stream().filter(item -> item.getDueAt() != null && item.getDueAt().isBefore(now)).count();
        int dueSoon = (int) openItems.stream().filter(item -> item.getDueAt() != null && !item.getDueAt().isBefore(now) && !item.getDueAt().isAfter(now.plus(stageRoute.horizonHours(), ChronoUnit.HOURS))).count();
        int blocking = (int) openItems.stream().filter(WorkItem::isBlocking).count();
        boolean escalationRequired = overdue > 0 || blocking > 0 || dueSoon >= runtimePolicy.dueSoonPressureThreshold();
        String band = overdue >= runtimePolicy.dueSoonPressureThreshold() || blocking >= 2 ? "CRITICA" : overdue > 0 || dueSoon >= runtimePolicy.dueSoonPressureThreshold() ? "PRESSAO" : openItems.isEmpty() ? "LIVRE" : "EQUILIBRADA";
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("SLA avaliado sobre work items ativos do processo na fila operacional da secretaria.");
        fundamentos.add("Stage analisado: " + stageRoute.stageToken() + ", banda: " + band + '.');
        fundamentos.add("Perfil judicial aplicado: " + runtimePolicy.profileCode() + " / " + runtimePolicy.branchClass() + '.');
        if (escalationRequired) {
            fundamentos.add("Escalonamento recomendado por atraso, bloqueio ou volume no horizonte do stage.");
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("openItems", openItems.size());
        metrics.put("overdueItems", overdue);
        metrics.put("dueSoonItems", dueSoon);
        metrics.put("blockingItems", blocking);
        metrics.put("stage", stageRoute.stageToken());
        metrics.put("coordinationQueueCode", stageRoute.coordinationQueueCode());
        metrics.put("coordinationInboxKey", stageRoute.coordinationInboxKey());
        metrics.put("band", band);
        metrics.put("escalationRequired", escalationRequired);
        metrics.put("scaleProfile", runtimePolicy.profileCode());
        metrics.put("scaleBranchClass", runtimePolicy.branchClass());
        metrics.put("runtimePriorityWeight", runtimePolicy.eventPriorityWeight());
        metrics.put("runtimeEscalationHours", runtimePolicy.slaEscalationHours());
        metrics.put("runtimeFollowUpHours", runtimePolicy.followUpEscalationHours());
        metrics.put("runtimeDueSoonThreshold", runtimePolicy.dueSoonPressureThreshold());
        return new SlaSnapshot(stageRoute.stageToken(), band, escalationRequired, openItems.stream().map(WorkItem::getId).toList(), List.copyOf(fundamentos), Map.copyOf(metrics));
    }

    @Transactional
    public EscalationSnapshot escalar(Processo processo,
                                      Usuario actor,
                                      SecretariatOperationalRoutingProfile routing,
                                      String stage) {
        JudicialScaleRuntimePolicyService.JudicialRuntimePolicy runtimePolicy = runtimePolicyService.resolve(routing);
        SlaSnapshot evaluation = avaliar(processo, routing, stage);
        StageSlaRoute stageRoute = resolveStageRoute(routing, stage, runtimePolicy);
        String templateCode = "SECRETARIA:ESCALONAMENTO:" + routing.routeKey() + ':' + stageRoute.stageToken() + ':' + processo.getId();
        WorkItem item = workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode).orElseGet(() -> WorkItem.builder()
                .processo(processo)
                .templateCode(templateCode)
                .build());
        item.setFaseOrigem(processo.getFaseAtual());
        item.setType(WorkItemType.DILIGENCIA);
        item.setTitulo("Escalonamento operacional — " + stageRoute.stageToken() + " — " + routing.secretariatCode());
        item.setDescricao(buildDescription(actor, routing, evaluation));
        item.setQueueCode(stageRoute.coordinationQueueCode());
        item.setInboxKey(stageRoute.coordinationInboxKey());
        item.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(evaluation.escalationRequired() ? 1 : 2);
        item.setBlocking(evaluation.escalationRequired());
        item.setUf(processo.getUf());
        item.setComarca(processo.getComarca());
        item.setBaseLegal("Escalonamento interno da secretaria para preservar SLA, evitar fila oculta e redistribuir carga operacional.");
        item.setDueAt(Instant.now().plus(evaluation.escalationRequired() ? runtimePolicy.slaEscalationHours() : runtimePolicy.followUpEscalationHours(), ChronoUnit.HOURS));
        WorkItem saved = workItemRepository.save(item);
        projectionService.upsert(saved, evaluation.escalationRequired() ? 180 : 110, List.of("ESCALONAMENTO", stageRoute.stageToken(), routing.secretariatCode(), evaluation.band()));
        return new EscalationSnapshot(saved.getId(), evaluation);
    }

    private String buildDescription(Usuario actor,
                                    SecretariatOperationalRoutingProfile routing,
                                    SlaSnapshot evaluation) {
        List<String> lines = new ArrayList<>();
        lines.add("Ator institucional: " + actor.getNome() + " (#" + actor.getId() + ")");
        lines.add("Secretaria: " + routing.secretariatCode());
        lines.add("Trilha: " + routing.organizationalPath());
        lines.add("Banda de SLA: " + evaluation.band());
        lines.add("Fundamentos: " + String.join(" | ", evaluation.fundamentos()));
        lines.add("Itens afetados: " + evaluation.affectedWorkItemIds());
        return String.join("\n", lines);
    }

    private boolean matchesStage(WorkItem item, StageSlaRoute stageRoute) {
        if (same(item.getInboxKey(), stageRoute.inboxKey()) && same(item.getQueueCode(), stageRoute.queueCode())) {
            return true;
        }
        return same(item.getInboxKey(), stageRoute.inboxKey());
    }

    private StageSlaRoute resolveStageRoute(SecretariatOperationalRoutingProfile routing, String stage, JudicialScaleRuntimePolicyService.JudicialRuntimePolicy runtimePolicy) {
        String normalized = normalizeStage(stage);
        String coordinationQueue = coordinationQueue(routing, normalized);
        String coordinationInbox = coordinationInbox(routing);
        Map<String, Object> tribunalFlow = tribunalFlow(routing);
        String tribunalInbox = firstNonBlank(stringValue(tribunalFlow.get("inboxKey")), coordinationInbox, routing.receiptInboxKey());
        Map<String, Object> queueCodes = nestedMap(tribunalFlow.get("queueCodes"));
        return switch (normalized) {
            case "RECEBIMENTO" -> new StageSlaRoute(normalized, routing.receiptInboxKey(), routing.receiptQueueCode(), coordinationInbox, coordinationQueue, Math.max(2L, runtimePolicy.scaleSlaHorizonHours(Math.max(2L, routing.receiptSla().toHours()))));
            case "SANEAMENTO" -> new StageSlaRoute(normalized, routing.saneamentoInboxKey(), routing.saneamentoQueueCode(), coordinationInbox, coordinationQueue, Math.max(4L, runtimePolicy.scaleSlaHorizonHours(Math.max(4L, routing.saneamentoSla().toHours()))));
            case "AUDIENCIA" -> new StageSlaRoute(normalized, routing.audienceInboxKey(), routing.audienceQueueCode(), coordinationInbox, coordinationQueue, Math.max(2L, runtimePolicy.scaleSlaHorizonHours(Math.max(2L, routing.audiencePreparationSla().toHours()))));
            case "ADMISSIBILIDADE" -> new StageSlaRoute(normalized, tribunalInbox, firstNonBlank(stringValue(queueCodes.get("admissibilidade")), routing.saneamentoQueueCode()), coordinationInbox, coordinationQueue, Math.max(6L, runtimePolicy.scaleSlaHorizonHours(6L)));
            case "PAUTA" -> new StageSlaRoute(normalized, tribunalInbox, firstNonBlank(stringValue(queueCodes.get("pauta")), stringValue(queueCodes.get("publicacaoPauta")), routing.audienceQueueCode()), coordinationInbox, coordinationQueue, Math.max(12L, runtimePolicy.scaleSlaHorizonHours(12L)));
            case "SUSTENTACAO_ORAL" -> new StageSlaRoute(normalized, tribunalInbox, firstNonBlank(stringValue(queueCodes.get("sustentacaoOral")), stringValue(queueCodes.get("pauta")), routing.audienceQueueCode()), coordinationInbox, coordinationQueue, Math.max(12L, runtimePolicy.scaleSlaHorizonHours(12L)));
            case "ACORDAO" -> new StageSlaRoute(normalized, tribunalInbox, firstNonBlank(stringValue(queueCodes.get("acordao")), routing.executionQueueCode()), coordinationInbox, coordinationQueue, Math.max(18L, runtimePolicy.scaleSlaHorizonHours(18L)));
            case "BAIXA_ORIGEM" -> new StageSlaRoute(normalized, tribunalInbox, firstNonBlank(stringValue(queueCodes.get("baixaOrigem")), routing.executionQueueCode()), coordinationInbox, coordinationQueue, Math.max(10L, runtimePolicy.scaleSlaHorizonHours(10L)));
            default -> new StageSlaRoute("EXECUCAO", routing.executionInboxKey(), routing.executionQueueCode(), coordinationInbox, coordinationQueue, Math.max(8L, runtimePolicy.scaleSlaHorizonHours(8L)));
        };
    }

    private String coordinationQueue(SecretariatOperationalRoutingProfile routing, String stage) {
        Object topologyObject = routing.metadata().get("topology");
        if (topologyObject instanceof Map<?, ?> topologyMap) {
            Object value = topologyMap.get("coordinationDesk");
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return routing.receiptQueueCode() + ":ESCALONAMENTO:" + stage;
    }

    private String coordinationInbox(SecretariatOperationalRoutingProfile routing) {
        Object topologyObject = routing.metadata().get("topology");
        if (topologyObject instanceof Map<?, ?> topologyMap) {
            Object value = topologyMap.get("baseInboxKey");
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value);
            }
        }
        return routing.receiptInboxKey();
    }

    private String normalizeStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return "RECEBIMENTO";
        }
        String normalized = stage.trim().toUpperCase(Locale.ROOT).replace('Ç', 'C');
        if (normalized.startsWith("ADMISS")) {
            return "ADMISSIBILIDADE";
        }
        if (normalized.startsWith("SANE")) {
            return "SANEAMENTO";
        }
        if (normalized.startsWith("AUD")) {
            return "AUDIENCIA";
        }
        if (normalized.startsWith("PAUTA") || normalized.startsWith("SESSAO") || normalized.startsWith("SESSA")) {
            return "PAUTA";
        }
        if (normalized.startsWith("SUSTENT")) {
            return "SUSTENTACAO_ORAL";
        }
        if (normalized.startsWith("ACOR")) {
            return "ACORDAO";
        }
        if (normalized.startsWith("BAIXA") || normalized.startsWith("RETORNO")) {
            return "BAIXA_ORIGEM";
        }
        if (normalized.startsWith("EXEC")) {
            return "EXECUCAO";
        }
        return "RECEBIMENTO";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> tribunalFlow(SecretariatOperationalRoutingProfile routing) {
        Object value = routing.metadata().get("tribunalFlow");
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private static boolean same(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    public record SlaSnapshot(
            String stage,
            String band,
            boolean escalationRequired,
            List<Long> affectedWorkItemIds,
            List<String> fundamentos,
            Map<String, Object> metrics
    ) {
    }

    public record EscalationSnapshot(
            Long workItemId,
            SlaSnapshot evaluation
    ) {
    }

    private record StageSlaRoute(
            String stageToken,
            String inboxKey,
            String queueCode,
            String coordinationInboxKey,
            String coordinationQueueCode,
            long horizonHours
    ) {
    }
}
