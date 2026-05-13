package com.tcc.pjb.backend.service.juiz.topology;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.juiz.guardrails.JuizProcessoGuardRailService;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingProfile;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JuizGabineteQueueIsolationService {

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final JuizProcessoGuardRailService guardRailService;
    private final JuizGabineteRoutingResolver routingResolver;

    public JuizGabineteQueueIsolationService(CurrentUserService currentUserService,
                                             ProcessoRepository processoRepository,
                                             WorkItemRepository workItemRepository,
                                             JuizProcessoGuardRailService guardRailService,
                                             JuizGabineteRoutingResolver routingResolver) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.guardRailService = Objects.requireNonNull(guardRailService);
        this.routingResolver = Objects.requireNonNull(routingResolver);
    }

    @Transactional(readOnly = true)
    public QueueIsolationSnapshot avaliar(Long processoId) {
        Usuario actor = requireJudgeActor();
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        JuizProcessoGuardRailService.GuardRailSnapshot guard = guardRailService.avaliar(processoId, "ISOLAMENTO_FILA_GABINETE");
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        List<WorkItem> active = workItemRepository.findAllByProcesso(processoId).stream()
                .filter(this::isActive)
                .toList();

        List<QueueItemView> gabineteItems = new ArrayList<>();
        List<QueueItemView> assessoriaItems = new ArrayList<>();
        List<QueueItemView> secretariaItems = new ArrayList<>();
        List<QueueItemView> driftItems = new ArrayList<>();
        List<QueueSignal> signals = new ArrayList<>();

        WorkItem conflictingJudge = null;
        WorkItem assessoriaIncompativel = null;
        WorkItem drift = null;
        boolean actorHasCapture = false;
        boolean secretariatReturnPending = false;

        for (WorkItem item : active) {
            QueueItemView view = toView(item, actor, routing);
            if (view.axis().startsWith("GABINETE")) {
                gabineteItems.add(view);
                if (view.assignedToActor()) {
                    actorHasCapture = true;
                }
                if (view.assignedToAnotherMagistrate()) {
                    conflictingJudge = item;
                }
            } else if (view.axis().startsWith("ASSESSORIA")) {
                assessoriaItems.add(view);
                if (!view.compatible()) {
                    assessoriaIncompativel = item;
                }
            } else if (view.axis().startsWith("SECRETARIA")) {
                secretariaItems.add(view);
                if (view.blocking() || view.assignedRole() == null) {
                    secretariatReturnPending = true;
                }
            } else {
                driftItems.add(view);
                if (drift == null) {
                    drift = item;
                }
            }
        }

        if (!guard.allowed()) {
            signals.add(signal("ATUACAO_MAGISTRADO", "CRITICA", false,
                    "Guard rails do processo bloquearam a atuação do gabinete para este magistrado."));
        } else {
            signals.add(signal("ATUACAO_MAGISTRADO", "INFO", true,
                    "Guard rails judiciais permanecem compatíveis com a topologia do gabinete."));
        }
        if (conflictingJudge != null) {
            signals.add(signal("CAPTURA_CONCORRENTE", "CRITICA", false,
                    "Existe captura ativa por outro magistrado em mesa incompatível: " + safe(conflictingJudge.getTitulo()) + '.'));
        } else if (actorHasCapture) {
            signals.add(signal("CAPTURA_ATIVA", "INFO", true,
                    "O processo já possui captura ativa no gabinete deste magistrado."));
        } else {
            signals.add(signal("CAPTURA_ATIVA", "ALTA", true,
                    "Não há captura ativa do magistrado na mesa topológica do gabinete."));
        }
        if (assessoriaIncompativel != null) {
            signals.add(signal("ASSESSORIA_INCOMPATIVEL", "ALTA", false,
                    "Há item de assessoria fora do desk compatível do gabinete: " + safe(assessoriaIncompativel.getTitulo()) + '.'));
        } else if (!assessoriaItems.isEmpty()) {
            signals.add(signal("ASSESSORIA_INCOMPATIVEL", "INFO", true,
                    "Itens de assessoria permanecem na lane correta do gabinete."));
        }
        if (drift != null) {
            signals.add(signal("DRIFT_FILA", "ALTA", false,
                    "Foi detectado item ativo fora da malha de gabinete/secretaria desta topologia: " + safe(drift.getQueueCode()) + '.'));
        } else {
            signals.add(signal("DRIFT_FILA", "INFO", true,
                    "Nenhum drift de fila foi detectado entre gabinete, assessoria e secretaria."));
        }
        if (secretariatReturnPending && actorHasCapture) {
            signals.add(signal("RETORNO_SECRETARIA", "MEDIA", false,
                    "Há item ainda pendente na secretaria enquanto a captura do gabinete está ativa."));
        } else if (!secretariaItems.isEmpty()) {
            signals.add(signal("RETORNO_SECRETARIA", "INFO", true,
                    "Fila de secretaria alinhada com a topologia de retorno do gabinete."));
        }

        String recommendedAction = resolveRecommendedAction(guard.allowed(), actorHasCapture, conflictingJudge, drift, assessoriaIncompativel, secretariatReturnPending);
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("gabineteItems", gabineteItems.size());
        metrics.put("assessoriaItems", assessoriaItems.size());
        metrics.put("secretariaItems", secretariaItems.size());
        metrics.put("driftItems", driftItems.size());
        metrics.put("captureOwnedByActor", actorHasCapture);
        metrics.put("secretariatReturnPending", secretariatReturnPending);
        metrics.put("recommendedAction", recommendedAction);
        metrics.put("routingKey", routing.routeKey());
        metrics.put("organizationalPath", routing.organizationalPath());

        return new QueueIsolationSnapshot(
                actor.getId(),
                actor.getNome(),
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                routing,
                guard,
                List.copyOf(gabineteItems),
                List.copyOf(assessoriaItems),
                List.copyOf(secretariaItems),
                List.copyOf(driftItems),
                List.copyOf(signals),
                recommendedAction,
                Map.copyOf(metrics)
        );
    }

    @Transactional(readOnly = true)
    public List<WorkItem> filtrarInboxCompativel(Usuario actor, List<WorkItem> inbox) {
        Usuario effectiveActor = actor == null ? requireJudgeActor() : actor;
        if (inbox == null || inbox.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<WorkItem> filtered = new ArrayList<>();
        for (WorkItem item : inbox) {
            CompatibilityVerdict verdict = avaliarCompatibilidade(effectiveActor, item);
            if (verdict.compatible() && item.getId() != null && seen.add(item.getId())) {
                filtered.add(item);
            }
        }
        return List.copyOf(filtered);
    }

    @Transactional(readOnly = true)
    public CompatibilityVerdict avaliarCompatibilidade(Usuario actor, WorkItem item) {
        if (actor == null || !actor.isMagistrado()) {
            return new CompatibilityVerdict(false, "ATOR_INVALIDO", Map.of("reason", "Usuário não pertence à magistratura."));
        }
        if (item == null || item.getProcesso() == null) {
            return new CompatibilityVerdict(false, "ITEM_SEM_PROCESSO", Map.of("reason", "Work item sem processo vinculado."));
        }
        Processo processo = item.getProcesso();
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        SecretariatOperationalRoutingProfile secretariat = routing.secretariatRouting();

        boolean magistrateOwnershipAllowed = item.getAssignedUser() == null
                || Objects.equals(item.getAssignedUser().getId(), actor.getId())
                || !item.getAssignedUser().isMagistrado();
        boolean territorialMatch = territoryMatches(actor, processo, item);
        boolean cabinetDeskMatch = matchesAny(item.getQueueCode(), routing.gabineteDesk(), routing.advisoryDesk(), routing.hearingDesk(), routing.coordinationDesk(), routing.redistributionDesk())
                || matchesAny(item.getInboxKey(), routing.gabineteInboxKey());
        boolean secretariatDeskMatch = matchesAny(item.getQueueCode(), secretariat.receiptQueueCode(), secretariat.saneamentoQueueCode(), secretariat.audienceQueueCode(), secretariat.executionQueueCode())
                || matchesAny(item.getInboxKey(), secretariat.receiptInboxKey(), secretariat.saneamentoInboxKey(), secretariat.audienceInboxKey(), secretariat.executionInboxKey());
        boolean activeStatus = isActive(item);
        boolean roleCompatible = item.getAssignedRole() == null
                || item.getAssignedRole().isMagistratura()
                || item.getAssignedRole().isAssessor()
                || matchesAny(item.getQueueCode(), routing.advisoryDesk(), routing.hearingDesk(), routing.coordinationDesk());
        boolean compatible = activeStatus && territorialMatch && magistrateOwnershipAllowed && roleCompatible && (cabinetDeskMatch || secretariatDeskMatch);

        LinkedHashMap<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("activeStatus", activeStatus);
        evidence.put("territorialMatch", territorialMatch);
        evidence.put("magistrateOwnershipAllowed", magistrateOwnershipAllowed);
        evidence.put("roleCompatible", roleCompatible);
        evidence.put("cabinetDeskMatch", cabinetDeskMatch);
        evidence.put("secretariatDeskMatch", secretariatDeskMatch);
        evidence.put("gabineteDesk", routing.gabineteDesk());
        evidence.put("gabineteInboxKey", routing.gabineteInboxKey());
        evidence.put("secretariatCode", secretariat.secretariatCode());
        evidence.entrySet().removeIf(entry -> entry.getValue() == null);
        String reason = compatible ? "COMPATIVEL" : resolveCompatibilityReason(activeStatus, territorialMatch, magistrateOwnershipAllowed, roleCompatible, cabinetDeskMatch, secretariatDeskMatch);
        return new CompatibilityVerdict(compatible, reason, Map.copyOf(evidence));
    }

    private String resolveRecommendedAction(boolean guardAllowed,
                                            boolean actorHasCapture,
                                            WorkItem conflictingJudge,
                                            WorkItem drift,
                                            WorkItem assessoriaIncompativel,
                                            boolean secretariatReturnPending) {
        if (!guardAllowed) {
            return "BLOQUEAR_ATUACAO";
        }
        if (conflictingJudge != null) {
            return "AGUARDAR_LIBERACAO_MAGISTRADO_COMPETENTE";
        }
        if (drift != null) {
            return "ESTABILIZAR_FILA_TOPOLOGICA";
        }
        if (assessoriaIncompativel != null) {
            return "REORGANIZAR_ASSESSORIA";
        }
        if (!actorHasCapture) {
            return "CAPTURAR_PROCESSO";
        }
        if (secretariatReturnPending) {
            return "SINCRONIZAR_SECRETARIA_E_GABINETE";
        }
        return "FLUXO_ISOLADO_E_ESTAVEL";
    }

    private String resolveCompatibilityReason(boolean activeStatus,
                                              boolean territorialMatch,
                                              boolean magistrateOwnershipAllowed,
                                              boolean roleCompatible,
                                              boolean cabinetDeskMatch,
                                              boolean secretariatDeskMatch) {
        if (!activeStatus) {
            return "STATUS_INATIVO";
        }
        if (!territorialMatch) {
            return "TERRITORIO_INCOMPATIVEL";
        }
        if (!magistrateOwnershipAllowed) {
            return "CAPTURA_POR_OUTRO_MAGISTRADO";
        }
        if (!roleCompatible) {
            return "ROLE_INCOMPATIVEL";
        }
        if (!cabinetDeskMatch && !secretariatDeskMatch) {
            return "FORA_DA_TOPOLOGIA_GABINETE_SECRETARIA";
        }
        return "INCOMPATIBILIDADE_NAO_CLASSIFICADA";
    }

    private QueueItemView toView(WorkItem item, Usuario actor, JuizGabineteRoutingProfile routing) {
        CompatibilityVerdict verdict = avaliarCompatibilidade(actor, item);
        String axis = resolveAxis(item, routing);
        Usuario assignedUser = item.getAssignedUser();
        return new QueueItemView(
                item.getId(),
                item.getTemplateCode(),
                item.getTitulo(),
                axis,
                item.getQueueCode(),
                item.getInboxKey(),
                assignedUser == null ? null : assignedUser.getId(),
                assignedUser == null ? null : assignedUser.getNome(),
                item.getAssignedRole() == null ? null : item.getAssignedRole().name(),
                item.getStatus() == null ? null : item.getStatus().name(),
                item.getDueAt(),
                item.isBlocking(),
                verdict.compatible(),
                Objects.equals(assignedUser == null ? null : assignedUser.getId(), actor.getId()),
                assignedUser != null && assignedUser.isMagistrado() && !Objects.equals(assignedUser.getId(), actor.getId()),
                verdict.evidence()
        );
    }

    private String resolveAxis(WorkItem item, JuizGabineteRoutingProfile routing) {
        SecretariatOperationalRoutingProfile secretariat = routing.secretariatRouting();
        if (matchesAny(item.getQueueCode(), routing.gabineteDesk()) || matchesAny(item.getInboxKey(), routing.gabineteInboxKey())) {
            return "GABINETE_CAPTURA";
        }
        if (matchesAny(item.getQueueCode(), routing.advisoryDesk(), routing.hearingDesk(), routing.coordinationDesk(), routing.redistributionDesk())) {
            return "ASSESSORIA_GABINETE";
        }
        if (matchesAny(item.getQueueCode(), secretariat.receiptQueueCode(), secretariat.saneamentoQueueCode(), secretariat.audienceQueueCode(), secretariat.executionQueueCode())
                || matchesAny(item.getInboxKey(), secretariat.receiptInboxKey(), secretariat.saneamentoInboxKey(), secretariat.audienceInboxKey(), secretariat.executionInboxKey())) {
            return "SECRETARIA_TOPOLOGICA";
        }
        return "FORA_DA_TOPOLOGIA";
    }

    private boolean territoryMatches(Usuario actor, Processo processo, WorkItem item) {
        String actorUf = normalize(firstNonBlank(actor.getUf()));
        String actorComarca = normalize(firstNonBlank(actor.getComarca()));
        String processoUf = normalize(firstNonBlank(item.getUf(), processo.getUf()));
        String processoComarca = normalize(firstNonBlank(item.getComarca(), processo.getComarca()));
        boolean ufOk = actorUf.isEmpty() || processoUf.isEmpty() || Objects.equals(actorUf, processoUf);
        boolean comarcaOk = actorComarca.isEmpty() || processoComarca.isEmpty() || Objects.equals(actorComarca, processoComarca);
        return ufOk && comarcaOk;
    }

    private boolean isActive(WorkItem item) {
        if (item == null || item.getStatus() == null) {
            return false;
        }
        return item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO;
    }

    private boolean matchesAny(String candidate, String... expected) {
        String normalizedCandidate = normalize(candidate);
        if (normalizedCandidate.isEmpty() || expected == null || expected.length == 0) {
            return false;
        }
        for (String value : expected) {
            if (!normalize(value).isEmpty() && Objects.equals(normalizedCandidate, normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private QueueSignal signal(String code, String level, boolean satisfied, String message) {
        boolean blocking = "CRITICA".equals(level) || "ALTA".equals(level);
        return new QueueSignal(code, level, blocking, satisfied, message);
    }

    private Usuario requireJudgeActor() {
        Usuario actor = currentUserService.getRequired();
        if (!actor.isMagistrado()) {
            throw new AccessDeniedPjbException("Apenas magistratura pode avaliar o isolamento do gabinete.");
        }
        return actor;
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

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private String safe(String raw) {
        return raw == null || raw.isBlank() ? "N/A" : raw.trim();
    }

    public record CompatibilityVerdict(
            boolean compatible,
            String reason,
            Map<String, Object> evidence
    ) {
    }

    public record QueueIsolationSnapshot(
            Long actorId,
            String actorNome,
            Long processoId,
            String numeroProcesso,
            JuizGabineteRoutingProfile routing,
            JuizProcessoGuardRailService.GuardRailSnapshot guardRail,
            List<QueueItemView> gabineteItems,
            List<QueueItemView> assessoriaItems,
            List<QueueItemView> secretariaItems,
            List<QueueItemView> driftItems,
            List<QueueSignal> signals,
            String recommendedAction,
            Map<String, Object> metrics
    ) {
    }

    public record QueueItemView(
            Long id,
            String templateCode,
            String titulo,
            String axis,
            String queueCode,
            String inboxKey,
            Long assignedUserId,
            String assignedUserNome,
            String assignedRole,
            String status,
            Instant dueAt,
            boolean blocking,
            boolean compatible,
            boolean assignedToActor,
            boolean assignedToAnotherMagistrate,
            Map<String, Object> evidence
    ) {
    }

    public record QueueSignal(
            String code,
            String level,
            boolean blocking,
            boolean satisfied,
            String message
    ) {
    }
}
