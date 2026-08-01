package com.tcc.pjb.backend.service.institutional.workbench;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchActionResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchExplainabilityResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchOperationalQueueResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchQueueItemResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchQuickActionsResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
public class InstitutionalWorkbenchProjectionService {

    private final CurrentUserService currentUserService;
    private final PainelServiceCommons painelServiceCommons;
    private final ProcessoRepository processoRepository;
    private final InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService;

    public InstitutionalWorkbenchProjectionService(CurrentUserService currentUserService,
                                                   PainelServiceCommons painelServiceCommons,
                                                   ProcessoRepository processoRepository,
                                                   InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService) {
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.painelServiceCommons = Objects.requireNonNull(painelServiceCommons, "painelServiceCommons");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.institutionalMaterialActionGuardService = Objects.requireNonNull(institutionalMaterialActionGuardService, "institutionalMaterialActionGuardService");
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "institutional.workbench.quick-actions.read", maxMillis = 1200, critical = false)
    public InstitutionalWorkbenchQuickActionsResponse quickActions(Long processoId) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = loadScopedProcess(processoId);
        List<ActionBlueprint> blueprints = actionBlueprints(usuario.getTipoUsuario());
        List<InstitutionalWorkbenchActionResponse> actions = new ArrayList<>();
        for (ActionBlueprint blueprint : blueprints) {
            actions.add(projectAction(blueprint, processo));
        }
        List<String> warnings = new ArrayList<>();
        if (processo == null) {
            warnings.add("Quick actions geradas em modo institucional geral. Informe um processo para validação material precisa.");
        }
        if (actions.stream().noneMatch(InstitutionalWorkbenchActionResponse::enabled)) {
            warnings.add("Nenhuma quick action saiu habilitada para o contexto atual.");
        }
        return new InstitutionalWorkbenchQuickActionsResponse(
                Instant.now(),
                actorClassLabel(usuario.getTipoUsuario()),
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                List.copyOf(actions),
                List.copyOf(new LinkedHashSet<>(warnings))
        );
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "institutional.workbench.action-preview.read", maxMillis = 1200, critical = false)
    public InstitutionalWorkbenchActionResponse previewAction(Long processoId, String actionCode) {
        Processo processo = loadScopedProcess(processoId);
        return previewAction(processo, actionCode);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "institutional.workbench.explainability.read", maxMillis = 1200, critical = false)
    public InstitutionalWorkbenchExplainabilityResponse previewExplainability(Long processoId, String actionCode) {
        Processo processo = loadScopedProcess(processoId);
        return previewExplainability(processo, actionCode);
    }

    InstitutionalWorkbenchActionResponse previewAction(Processo processo, String actionCode) {
        ActionBlueprint blueprint = actionBlueprintFor(actionCode);
        return projectAction(blueprint, processo);
    }

    InstitutionalWorkbenchExplainabilityResponse previewExplainability(Processo processo, String actionCode) {
        ActionBlueprint blueprint = actionBlueprintFor(actionCode);
        InstitutionalMaterialActionGuardService.GuardDecision decision = processo != null
                ? institutionalMaterialActionGuardService.analyzeProcessAction(processo, blueprint.action())
                : institutionalMaterialActionGuardService.analyzeCatalogAction(blueprint.action(), blueprint.catalogContext());
        return explainability(decision);
    }

    public String currentActorClass() {
        return actorClassLabel(currentUserService.getRequired().getTipoUsuario());
    }

    private Processo loadScopedProcess(Long processoId) {
        if (processoId == null) {
            return null;
        }
        return processoRepository.findWorkspaceScopedById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private ActionBlueprint actionBlueprintFor(String actionCode) {
        Usuario usuario = currentUserService.getRequired();
        return actionBlueprints(usuario.getTipoUsuario()).stream()
                .filter(candidate -> candidate.code().name().equalsIgnoreCase(actionCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Quick action institucional não encontrada: " + actionCode));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "institutional.workbench.operational-queue.read", maxMillis = 1500, critical = false)
    public InstitutionalWorkbenchOperationalQueueResponse operationalQueue(int limit) {
        Usuario usuario = currentUserService.getRequired();
        int sanitizedLimit = Math.max(1, Math.min(limit, 50));
        List<WorkItem> inbox = painelServiceCommons.inboxHibrido(usuario, sanitizedLimit);
        List<InstitutionalWorkbenchQueueItemResponse> items = inbox.stream()
                .map(this::projectQueueItem)
                .sorted(queueComparator())
                .toList();
        int actionable = (int) items.stream().filter(item -> item.primaryAction() != null && item.primaryAction().enabled()).count();
        int blocked = (int) items.stream().filter(item -> item.primaryAction() != null && !item.primaryAction().enabled()).count();
        List<String> warnings = new ArrayList<>();
        if (inbox.size() >= sanitizedLimit) {
            warnings.add("A fila operacional foi truncada no limite solicitado. Refine por processo, unidade ou rota institucional para leitura mais precisa.");
        }
        if (items.isEmpty()) {
            warnings.add("Nenhum work item operacional pendente foi encontrado para a malha atual.");
        }
        return new InstitutionalWorkbenchOperationalQueueResponse(
                Instant.now(),
                actorClassLabel(usuario.getTipoUsuario()),
                sanitizedLimit,
                items.size(),
                actionable,
                blocked,
                items,
                List.copyOf(new LinkedHashSet<>(warnings))
        );
    }

    private InstitutionalWorkbenchQueueItemResponse projectQueueItem(WorkItem item) {
        Processo processo = item.getProcesso();
        InstitutionalWorkbenchActionResponse primaryAction = null;
        List<InstitutionalWorkbenchActionResponse> allowed = List.of();
        List<InstitutionalWorkbenchActionResponse> blocked = List.of();
        InstitutionalWorkbenchExplainabilityResponse explainability;
        if (processo == null) {
            explainability = new InstitutionalWorkbenchExplainabilityResponse(
                    actorClassLabel(currentUserService.getRequired().getTipoUsuario()),
                    "INDETERMINADA",
                    "REVIEW",
                    List.of("Work item sem processo vinculado. A atuação material exige revisão institucional prévia."),
                    List.of("Associe o work item a um processo para liberação plena das ações."),
                    Map.of("hasProcesso", false)
            );
        } else {
            List<ActionBlueprint> blueprints = actionBlueprints(currentUserService.getRequired().getTipoUsuario());
            ArrayList<InstitutionalWorkbenchActionResponse> projected = new ArrayList<>();
            for (ActionBlueprint blueprint : blueprints) {
                projected.add(projectAction(blueprint, processo));
            }
            allowed = projected.stream().filter(InstitutionalWorkbenchActionResponse::enabled).toList();
            blocked = projected.stream().filter(action -> !action.enabled()).toList();
            primaryAction = allowed.isEmpty() ? projected.stream().findFirst().orElse(null) : allowed.get(0);
            InstitutionalMaterialActionGuardService.GuardDecision decision = primaryAction == null
                    ? null
                    : institutionalMaterialActionGuardService.analyzeProcessAction(processo, MaterialActionCode.valueOf(primaryAction.code()).action());
            explainability = decision == null
                    ? new InstitutionalWorkbenchExplainabilityResponse(
                            actorClassLabel(currentUserService.getRequired().getTipoUsuario()),
                            "INDETERMINADA",
                            "REVIEW",
                            List.of("Nenhuma ação material principal foi derivada para o item."),
                            List.of(),
                            Map.of("hasPrimaryAction", false)
                    )
                    : explainability(decision);
        }
        return new InstitutionalWorkbenchQueueItemResponse(
                item.getId(),
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                item.getTitulo(),
                item.getQueueCode(),
                item.getStatus() != null ? item.getStatus().name() : null,
                item.getPrioridade(),
                item.getDueAt(),
                primaryAction,
                allowed,
                blocked,
                explainability
        );
    }

    private InstitutionalWorkbenchActionResponse projectAction(ActionBlueprint blueprint, Processo processo) {
        InstitutionalMaterialActionGuardService.GuardDecision decision = processo != null
                ? institutionalMaterialActionGuardService.analyzeProcessAction(processo, blueprint.action())
                : institutionalMaterialActionGuardService.analyzeCatalogAction(blueprint.action(), blueprint.catalogContext());
        return new InstitutionalWorkbenchActionResponse(
                blueprint.action().name(),
                blueprint.label(),
                blueprint.route(processo),
                blueprint.method(),
                decision.verdict() == InstitutionalMaterialActionGuardService.Verdict.ALLOW,
                decision.verdict().name(),
                severity(decision.verdict()),
                redirectRoute(decision, blueprint),
                decision.reasons(),
                decision.warnings(),
                decision.metrics()
        );
    }

    private InstitutionalWorkbenchExplainabilityResponse explainability(InstitutionalMaterialActionGuardService.GuardDecision decision) {
        return new InstitutionalWorkbenchExplainabilityResponse(
                decision.actorBranch().name(),
                decision.targetSphere().name(),
                decision.verdict().name(),
                decision.reasons(),
                decision.warnings(),
                decision.metrics()
        );
    }

    private Comparator<InstitutionalWorkbenchQueueItemResponse> queueComparator() {
        return Comparator
                .comparing(InstitutionalWorkbenchQueueItemResponse::dueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InstitutionalWorkbenchQueueItemResponse::prioridade, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InstitutionalWorkbenchQueueItemResponse::workItemId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private String redirectRoute(InstitutionalMaterialActionGuardService.GuardDecision decision, ActionBlueprint blueprint) {
        if (decision.verdict() != InstitutionalMaterialActionGuardService.Verdict.BLOCK_WITH_REDIRECT) {
            return null;
        }
        return switch (decision.targetSphere()) {
            case FEDERAL -> "/api/v1/institucional/workbench?scope=FEDERAL";
            case ESTADUAL -> "/api/v1/institucional/workbench?scope=ESTADUAL";
            case MUNICIPAL -> "/api/v1/institucional/workbench?scope=MUNICIPAL";
            case INDETERMINADA -> blueprint.route(null);
        };
    }

    private String actorClassLabel(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return "INSTITUCIONAL";
        }
        return switch (tipoUsuario) {
            case DELEGADO_POLICIA -> "DELEGACIA_ESTADUAL";
            case DELEGADO_POLICIA_FEDERAL -> "POLICIA_FEDERAL";
            case MEMBRO_MINISTERIO_PUBLICO -> "MINISTERIO_PUBLICO_ESTADUAL";
            case PROMOTOR_ELEITORAL -> "MINISTERIO_PUBLICO_ELEITORAL";
            case PROMOTOR_TRABALHISTA -> "MINISTERIO_PUBLICO_TRABALHISTA";
            case PROCURADOR_GERAL_REPUBLICA -> "MINISTERIO_PUBLICO_FEDERAL";
            case DEFENSOR_PUBLICO -> "DEFENSORIA_ESTADUAL";
            case DEFENSOR_PUBLICO_FEDERAL -> "DEFENSORIA_FEDERAL";
            case PROCURADORIA_MUNICIPAL -> "PROCURADORIA_MUNICIPAL";
            case PROCURADORIA_ESTADUAL -> "PROCURADORIA_ESTADUAL";
            case PROCURADORIA_FEDERAL, PROCURADOR -> "PROCURADORIA_FEDERAL";
            default -> tipoUsuario.name();
        };
    }

    private String severity(InstitutionalMaterialActionGuardService.Verdict verdict) {
        return switch (verdict) {
            case ALLOW -> "SUCCESS";
            case REVIEW -> "WARNING";
            case BLOCK, BLOCK_WITH_REDIRECT -> "DANGER";
        };
    }

    private List<ActionBlueprint> actionBlueprints(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return List.of();
        }
        return switch (tipoUsuario) {
            case DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL -> List.of(
                    ActionBlueprint.processAction(MaterialActionCode.DELEGADO_DILIGENCIA, "Registrar diligência", "/api/v1/delegado/requisicao/diligencia"),
                    ActionBlueprint.processAction(MaterialActionCode.DELEGADO_PECA_INQUERITO, "Registrar peça de inquérito", "/api/v1/delegado/inqueritos/{processoId}/peca-multimidia")
            );
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA -> List.of(
                    ActionBlueprint.processAction(MaterialActionCode.MINISTERIO_PUBLICO_MANIFESTACAO, "Registrar manifestação", "/api/v1/mp/manifestacao/{processoId}"),
                    ActionBlueprint.processAction(MaterialActionCode.MINISTERIO_PUBLICO_PARECER, "Emitir parecer", "/api/v1/mp/parecer/{processoId}"),
                    ActionBlueprint.processAction(MaterialActionCode.MINISTERIO_PUBLICO_RECURSO, "Interpor recurso", "/api/v1/recursal/processos/{processoId}/recurso"),
                    ActionBlueprint.processAction(MaterialActionCode.MINISTERIO_PUBLICO_REQUISICAO_DILIGENCIA, "Requisitar diligência", "/api/v1/mp/requisicao/diligencia/{processoId}")
            );
            case DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL -> List.of(
                    ActionBlueprint.processAction(MaterialActionCode.DEFENSORIA_PETICAO, "Protocolar petição", "/api/v1/defensor/peticao/{processoId}"),
                    ActionBlueprint.processAction(MaterialActionCode.DEFENSORIA_RECURSO, "Interpor recurso", "/api/v1/recursal/processos/{processoId}/recurso"),
                    ActionBlueprint.processAction(MaterialActionCode.DEFENSORIA_GRATUIDADE, "Requerer gratuidade", "/api/v1/defensor/gratuidade/{processoId}/requerimento")
            );
            case PROCURADORIA_MUNICIPAL, PROCURADORIA_ESTADUAL, PROCURADORIA_FEDERAL, PROCURADOR -> List.of(
                    ActionBlueprint.processAction(MaterialActionCode.PROCURADORIA_CONTESTACAO, "Apresentar contestação", "/api/v1/procuradoria/operacional/processos/{processoId}/contestacao"),
                    ActionBlueprint.processAction(MaterialActionCode.PROCURADORIA_PARECER, "Emitir parecer", "/api/v1/procuradoria/operacional/processos/{processoId}/parecer"),
                    ActionBlueprint.processAction(MaterialActionCode.PROCURADORIA_RECURSO, "Interpor recurso", "/api/v1/recursal/processos/{processoId}/recurso"),
                    ActionBlueprint.catalogAction(MaterialActionCode.PROCURADORIA_EXECUCAO_FISCAL, "Ajuizar execução fiscal", "/api/v1/procuradoria/operacional/execucao-fiscal",
                            new InstitutionalMaterialActionGuardService.CatalogActionContext(
                                    InstitutionalMaterialActionGuardService.TargetSphere.INDETERMINADA,
                                    null,
                                    RamoDireito.EXECUCAO_FISCAL,
                                    RitoProcessual.EXECUCAO_FISCAL,
                                    "EXECUCAO_FISCAL",
                                    "Execução fiscal institucional",
                                    false
                            ))
            );
            default -> List.of();
        };
    }

    private enum MaterialActionCode {
        DELEGADO_DILIGENCIA(InstitutionalMaterialActionGuardService.MaterialAction.DELEGADO_DILIGENCIA),
        DELEGADO_PECA_INQUERITO(InstitutionalMaterialActionGuardService.MaterialAction.DELEGADO_PECA_INQUERITO),
        MINISTERIO_PUBLICO_MANIFESTACAO(InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_MANIFESTACAO),
        MINISTERIO_PUBLICO_PARECER(InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_PARECER),
        MINISTERIO_PUBLICO_RECURSO(InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_RECURSO),
        MINISTERIO_PUBLICO_REQUISICAO_DILIGENCIA(InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_REQUISICAO_DILIGENCIA),
        DEFENSORIA_PETICAO(InstitutionalMaterialActionGuardService.MaterialAction.DEFENSORIA_PETICAO),
        DEFENSORIA_RECURSO(InstitutionalMaterialActionGuardService.MaterialAction.DEFENSORIA_RECURSO),
        DEFENSORIA_GRATUIDADE(InstitutionalMaterialActionGuardService.MaterialAction.DEFENSORIA_GRATUIDADE),
        PROCURADORIA_CONTESTACAO(InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_CONTESTACAO),
        PROCURADORIA_PARECER(InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_PARECER),
        PROCURADORIA_RECURSO(InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_RECURSO),
        PROCURADORIA_EXECUCAO_FISCAL(InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_EXECUCAO_FISCAL);

        private final InstitutionalMaterialActionGuardService.MaterialAction action;

        MaterialActionCode(InstitutionalMaterialActionGuardService.MaterialAction action) {
            this.action = action;
        }

        public InstitutionalMaterialActionGuardService.MaterialAction action() {
            return action;
        }
    }

    private record ActionBlueprint(MaterialActionCode code,
                                   String label,
                                   String routeTemplate,
                                   String method,
                                   InstitutionalMaterialActionGuardService.CatalogActionContext catalogContext,
                                   boolean processBound) {

        static ActionBlueprint processAction(MaterialActionCode code, String label, String routeTemplate) {
            return new ActionBlueprint(code, label, routeTemplate, "POST", InstitutionalMaterialActionGuardService.CatalogActionContext.empty(), true);
        }

        static ActionBlueprint catalogAction(MaterialActionCode code,
                                             String label,
                                             String routeTemplate,
                                             InstitutionalMaterialActionGuardService.CatalogActionContext catalogContext) {
            return new ActionBlueprint(code, label, routeTemplate, "POST", catalogContext, false);
        }

        InstitutionalMaterialActionGuardService.MaterialAction action() {
            return code.action();
        }

        String route(Processo processo) {
            if (!processBound) {
                return routeTemplate;
            }
            if (processo == null || processo.getId() == null) {
                return routeTemplate.replace("{processoId}", "{processoId}");
            }
            return routeTemplate.replace("{processoId}", String.valueOf(processo.getId()));
        }
    }
}
