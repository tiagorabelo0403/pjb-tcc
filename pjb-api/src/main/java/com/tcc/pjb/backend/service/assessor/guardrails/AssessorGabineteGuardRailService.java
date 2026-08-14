package com.tcc.pjb.backend.service.assessor.guardrails;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingProfile;
import com.tcc.pjb.backend.service.juiz.routing.JuizGabineteRoutingResolver;
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
public class AssessorGabineteGuardRailService {

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PainelServiceCommons commons;
    private final JuizGabineteRoutingResolver routingResolver;

    public AssessorGabineteGuardRailService(CurrentUserService currentUserService,
                                            ProcessoRepository processoRepository,
                                            WorkItemRepository workItemRepository,
                                            PainelServiceCommons commons,
                                            JuizGabineteRoutingResolver routingResolver) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.commons = Objects.requireNonNull(commons);
        this.routingResolver = Objects.requireNonNull(routingResolver);
    }

    @Transactional(readOnly = true)
    public List<WorkItem> filtrarInboxCompativel(Usuario assessor, List<WorkItem> inbox) {
        Usuario effectiveAssessor = assessor == null ? requireAssessorActor() : assessor;
        if (inbox == null || inbox.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<WorkItem> filtered = new ArrayList<>();
        for (WorkItem item : inbox) {
            CompatibilityVerdict verdict = avaliarCompatibilidade(effectiveAssessor, item);
            if (verdict.compatible() && item.getId() != null && seen.add(item.getId())) {
                filtered.add(item);
            }
        }
        return List.copyOf(filtered);
    }

    @Transactional(readOnly = true)
    public AssessorProcessoGuardRailSnapshot avaliarProcesso(Long processoId) {
        Usuario assessor = requireAssessorActor();
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        List<WorkItem> active = workItemRepository.findAllByProcesso(processoId).stream()
                .filter(this::isActive)
                .toList();

        List<AssessorWorkItemView> compatibleItems = new ArrayList<>();
        List<AssessorWorkItemView> incompatibleItems = new ArrayList<>();
        List<GuardSignal> signals = new ArrayList<>();

        boolean advisoryLanePresent = false;
        boolean judgeCapturePresent = false;
        boolean formalHandoffPresent = hasFormalAssessoriaHandoff(processoId);
        boolean sigiloReforcado = processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;

        for (WorkItem item : active) {
            CompatibilityVerdict verdict = avaliarCompatibilidade(assessor, item);
            if (matchesAssessoriaLane(item, routing)) {
                advisoryLanePresent = true;
            }
            if (item.getAssignedUser() != null && item.getAssignedUser().isMagistrado()) {
                judgeCapturePresent = true;
            }
            AssessorWorkItemView view = new AssessorWorkItemView(
                    item.getId(),
                    item.getTemplateCode(),
                    item.getTitulo(),
                    item.getQueueCode(),
                    item.getInboxKey(),
                    item.getAssignedUser() == null ? null : item.getAssignedUser().getId(),
                    item.getAssignedUser() == null ? null : item.getAssignedUser().getNome(),
                    item.getAssignedRole() == null ? null : item.getAssignedRole().name(),
                    item.getStatus() == null ? null : item.getStatus().name(),
                    item.getDueAt(),
                    verdict.compatible(),
                    verdict.reason(),
                    verdict.evidence()
            );
            if (verdict.compatible()) {
                compatibleItems.add(view);
            } else {
                incompatibleItems.add(view);
            }
        }

        if (!formalHandoffPresent) {
            signals.add(signal("HANDOFF_FORMAL", "ALTA", false,
                    "Não existe handoff formal ativo do gabinete para a assessoria deste processo."));
        } else {
            signals.add(signal("HANDOFF_FORMAL", "INFO", true,
                    "Handoff formal do gabinete para a assessoria encontrado."));
        }
        if (!advisoryLanePresent) {
            signals.add(signal("ASSESSORIA_SEM_LANE", "ALTA", false,
                    "Não existe lane ativa de assessoria compatível para este processo no gabinete."));
        } else {
            signals.add(signal("ASSESSORIA_SEM_LANE", "INFO", true,
                    "Há lane ativa de assessoria alinhada ao gabinete topológico."));
        }
        if (!judgeCapturePresent) {
            signals.add(signal("CAPTURA_JUDICIAL", "ALTA", false,
                    "O magistrado ainda não capturou o processo na mesa topológica do gabinete."));
        } else {
            signals.add(signal("CAPTURA_JUDICIAL", "INFO", true,
                    "Existe captura ou atuação judicial ativa vinculada ao gabinete."));
        }
        if (sigiloReforcado && compatibleItems.stream().noneMatch(AssessorWorkItemView::compatible)) {
            signals.add(signal("SIGILO_REFORCADO", "CRITICA", false,
                    "Processo sigiloso sem lane compatível de assessoria explicitamente rastreável."));
        } else if (sigiloReforcado) {
            signals.add(signal("SIGILO_REFORCADO", "INFO", true,
                    "Processo sigiloso com lane compatível de assessoria mantida na topologia correta."));
        }
        if (!incompatibleItems.isEmpty()) {
            signals.add(signal("DRIFT_ASSESSORIA", "ALTA", false,
                    "Há item de assessoria ou despacho fora da topologia do gabinete para este assessor."));
        } else {
            signals.add(signal("DRIFT_ASSESSORIA", "INFO", true,
                    "Os itens da assessoria permanecem na lane correta do gabinete."));
        }

        String recommendedAction = resolveRecommendedAction(formalHandoffPresent, advisoryLanePresent, judgeCapturePresent, sigiloReforcado, incompatibleItems.isEmpty(), compatibleItems.isEmpty());
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("compatibleItems", compatibleItems.size());
        metrics.put("incompatibleItems", incompatibleItems.size());
        metrics.put("formalHandoffPresent", formalHandoffPresent);
        metrics.put("advisoryLanePresent", advisoryLanePresent);
        metrics.put("judgeCapturePresent", judgeCapturePresent);
        metrics.put("sigiloReforcado", sigiloReforcado);
        metrics.put("recommendedAction", recommendedAction);
        metrics.put("territorialBase", normalize(firstNonBlank(assessor.getUf())) + ':' + normalize(firstNonBlank(assessor.getComarca())));

        LinkedHashMap<String, Object> gabinete = new LinkedHashMap<>();
        gabinete.put("gabineteDesk", routing.gabineteDesk());
        gabinete.put("advisoryDesk", routing.advisoryDesk());
        gabinete.put("hearingDesk", routing.hearingDesk());
        gabinete.put("coordinationDesk", routing.coordinationDesk());
        gabinete.put("redistributionDesk", routing.redistributionDesk());
        gabinete.put("gabineteInboxKey", routing.gabineteInboxKey());
        gabinete.put("secretariatCode", routing.secretariatRouting().secretariatCode());
        gabinete.put("activeItems", active.size());
        gabinete.put("advisoryLanePresent", advisoryLanePresent);
        gabinete.put("judgeCapturePresent", judgeCapturePresent);
        gabinete.entrySet().removeIf(entry -> entry.getValue() == null);

        return new AssessorProcessoGuardRailSnapshot(
                assessor.getId(),
                assessor.getNome(),
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                routing,
                Map.copyOf(gabinete),
                List.copyOf(compatibleItems),
                List.copyOf(incompatibleItems),
                List.copyOf(signals),
                recommendedAction,
                Map.copyOf(metrics)
        );
    }

    @Transactional(readOnly = true)
    public CompatibilityVerdict avaliarCompatibilidade(Usuario assessor, WorkItem item) {
        if (assessor == null || !assessor.isAssessor()) {
            return new CompatibilityVerdict(false, "ATOR_INVALIDO", Map.of("reason", "Usuário não pertence à assessoria."));
        }
        if (item == null || item.getProcesso() == null) {
            return new CompatibilityVerdict(false, "ITEM_SEM_PROCESSO", Map.of("reason", "Work item sem processo vinculado."));
        }
        Processo processo = item.getProcesso();
        JuizGabineteRoutingProfile routing = routingResolver.resolve(processo);
        boolean activeStatus = isActive(item);
        boolean territorialMatch = territoryMatches(assessor, processo, item);
        boolean advisoryLane = matchesAssessoriaLane(item, routing);
        boolean formalHandoff = startsWith(item.getTemplateCode(), "HANDOFF:GABINETE:ASSESSORIA:", "HANDOFF:ASSESSORIA:GABINETE:") || hasFormalAssessoriaHandoff(processo.getId());
        boolean roleCompatible = item.getAssignedUser() == null
                || Objects.equals(item.getAssignedUser().getId(), assessor.getId())
                || item.getAssignedUser().isMagistrado()
                || item.getAssignedUser().isAssessor();
        boolean secrecyCompatible = processo.getNivelSigilo() == null
                || processo.getNivelSigilo() == NivelSigilo.PUBLICO
                || advisoryLane
                || Objects.equals(item.getAssignedUser() == null ? null : item.getAssignedUser().getId(), assessor.getId());
        boolean compatible = activeStatus && territorialMatch && advisoryLane && formalHandoff && roleCompatible && secrecyCompatible;

        LinkedHashMap<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("activeStatus", activeStatus);
        evidence.put("territorialMatch", territorialMatch);
        evidence.put("advisoryLane", advisoryLane);
        evidence.put("formalHandoff", formalHandoff);
        evidence.put("roleCompatible", roleCompatible);
        evidence.put("secrecyCompatible", secrecyCompatible);
        evidence.put("advisoryDesk", routing.advisoryDesk());
        evidence.put("hearingDesk", routing.hearingDesk());
        evidence.put("coordinationDesk", routing.coordinationDesk());
        evidence.put("redistributionDesk", routing.redistributionDesk());
        evidence.put("gabineteInboxKey", routing.gabineteInboxKey());
        evidence.entrySet().removeIf(entry -> entry.getValue() == null);
        return new CompatibilityVerdict(compatible, resolveReason(activeStatus, territorialMatch, advisoryLane, formalHandoff, roleCompatible, secrecyCompatible), Map.copyOf(evidence));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarMinutasCompativeis() {
        Usuario assessor = requireAssessorActor();
        List<WorkItem> inbox = filtrarInboxCompativel(assessor, commons.inboxHibrido(assessor, 40));
        return inbox.stream()
                .filter(this::isMinutaLike)
                .map(commons::mapWorkItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarDespachosCompativeis() {
        Usuario assessor = requireAssessorActor();
        List<WorkItem> inbox = filtrarInboxCompativel(assessor, commons.inboxHibrido(assessor, 40));
        return inbox.stream()
                .filter(this::isDespachoLike)
                .map(commons::mapWorkItem)
                .toList();
    }

    private boolean isMinutaLike(WorkItem item) {
        return commons.titleContains(item, "MINUTA", "RASCUNHO", "DRAFT", "MEMORIAL", "DESPACHO");
    }

    private boolean isDespachoLike(WorkItem item) {
        return commons.titleContains(item, "DESPACHO", "DECISAO", "SENTENCA", "CONCLUSAO");
    }

    private boolean matchesAssessoriaLane(WorkItem item, JuizGabineteRoutingProfile routing) {
        return matchesAny(item.getQueueCode(), routing.advisoryDesk(), routing.hearingDesk(), routing.coordinationDesk(), routing.redistributionDesk())
                || matchesAny(item.getInboxKey(), routing.gabineteInboxKey())
                || item.getAssignedRole() != null && item.getAssignedRole().isAssessor();
    }

    private boolean territoryMatches(Usuario assessor, Processo processo, WorkItem item) {
        Comarca actorEntidade = assessor.getComarcaEntidade();
        Comarca processoEntidade = item.getComarcaEntidade() != null ? item.getComarcaEntidade() : processo.getComarcaEntidade();
        if (actorEntidade != null && processoEntidade != null) {
            return Objects.equals(actorEntidade.getId(), processoEntidade.getId());
        }
        String actorUf = normalize(firstNonBlank(assessor.getUf()));
        String actorComarca = normalize(firstNonBlank(assessor.getComarca()));
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

    private String resolveReason(boolean activeStatus,
                                 boolean territorialMatch,
                                 boolean advisoryLane,
                                 boolean formalHandoff,
                                 boolean roleCompatible,
                                 boolean secrecyCompatible) {
        if (!activeStatus) {
            return "STATUS_INATIVO";
        }
        if (!territorialMatch) {
            return "TERRITORIO_INCOMPATIVEL";
        }
        if (!advisoryLane) {
            return "FORA_DA_LANE_DE_ASSESSORIA";
        }
        if (!formalHandoff) {
            return "SEM_HANDOFF_FORMAL";
        }
        if (!roleCompatible) {
            return "ROLE_INCOMPATIVEL";
        }
        if (!secrecyCompatible) {
            return "SIGILO_INCOMPATIVEL";
        }
        return "COMPATIVEL";
    }

    private String resolveRecommendedAction(boolean formalHandoffPresent,
                                            boolean advisoryLanePresent,
                                            boolean judgeCapturePresent,
                                            boolean sigiloReforcado,
                                            boolean noIncompatibleItems,
                                            boolean noCompatibleItems) {
        if (!formalHandoffPresent) {
            return "AGUARDAR_HANDOFF_FORMAL_DO_GABINETE";
        }
        if (!judgeCapturePresent) {
            return "SOLICITAR_CAPTURA_DO_MAGISTRADO";
        }
        if (!advisoryLanePresent) {
            return "REORGANIZAR_LANE_DE_ASSESSORIA";
        }
        if (!noIncompatibleItems) {
            return "ESTABILIZAR_ITENS_DE_ASSESSORIA";
        }
        if (sigiloReforcado && noCompatibleItems) {
            return "BLOQUEAR_ATUACAO_ATE_LANE_EXPLICITA";
        }
        if (noCompatibleItems) {
            return "AGUARDAR_ENFILEIRAMENTO_COMPATIVEL";
        }
        return "ASSESSORIA_LIBERADA_PARA_MINUTACAO";
    }

    private GuardSignal signal(String code, String level, boolean satisfied, String message) {
        boolean blocking = "CRITICA".equals(level) || "ALTA".equals(level);
        return new GuardSignal(code, level, blocking, satisfied, message);
    }


    private boolean hasFormalAssessoriaHandoff(Long processoId) {
        return workItemRepository.findAllByProcesso(processoId).stream()
                .filter(this::isActive)
                .anyMatch(item -> startsWith(item.getTemplateCode(), "HANDOFF:GABINETE:ASSESSORIA:", "HANDOFF:ASSESSORIA:GABINETE:"));
    }

    private boolean startsWith(String candidate, String... prefixes) {
        if (candidate == null || prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && !prefix.isBlank() && candidate.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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

    private Usuario requireAssessorActor() {
        Usuario actor = currentUserService.getRequired();
        if (!actor.isAssessor()) {
            throw new AccessDeniedPjbException("Apenas assessoria pode operar o guard rail do gabinete." );
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

    public record CompatibilityVerdict(
            boolean compatible,
            String reason,
            Map<String, Object> evidence
    ) {
    }

    public record AssessorProcessoGuardRailSnapshot(
            Long actorId,
            String actorNome,
            Long processoId,
            String numeroProcesso,
            JuizGabineteRoutingProfile routing,
            Map<String, Object> gabinete,
            List<AssessorWorkItemView> compatibleItems,
            List<AssessorWorkItemView> incompatibleItems,
            List<GuardSignal> signals,
            String recommendedAction,
            Map<String, Object> metrics
    ) {
    }

    public record AssessorWorkItemView(
            Long id,
            String templateCode,
            String titulo,
            String queueCode,
            String inboxKey,
            Long assignedUserId,
            String assignedUserNome,
            String assignedRole,
            String status,
            Instant dueAt,
            boolean compatible,
            String reason,
            Map<String, Object> evidence
    ) {
    }

    public record GuardSignal(
            String code,
            String level,
            boolean blocking,
            boolean satisfied,
            String message
    ) {
    }
}
