package com.tcc.pjb.backend.service.institutional.workbench;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchActionPreviewResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchActionResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchExplainabilityResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchOperationalQueueResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchProfileResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchQuickActionsResponse;
import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchWorkspaceResponse;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalWorkbenchService {

    private final CurrentUserService currentUserService;
    private final InstitutionalWorkbenchProfileResolver profileResolver;
    private final InstitutionalWorkbenchWidgetPolicyEngine widgetPolicyEngine;
    private final InstitutionalWorkbenchProjectionService projectionService;
    private final ProcessoRepository processoRepository;

    public InstitutionalWorkbenchService(CurrentUserService currentUserService,
                                         InstitutionalWorkbenchProfileResolver profileResolver,
                                         InstitutionalWorkbenchWidgetPolicyEngine widgetPolicyEngine,
                                         InstitutionalWorkbenchProjectionService projectionService,
                                         ProcessoRepository processoRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.profileResolver = Objects.requireNonNull(profileResolver, "profileResolver");
        this.widgetPolicyEngine = Objects.requireNonNull(widgetPolicyEngine, "widgetPolicyEngine");
        this.projectionService = Objects.requireNonNull(projectionService, "projectionService");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
    }

    private Processo loadScopedProcess(Long processoId) {
        if (processoId == null) {
            return null;
        }
        return processoRepository.findWorkspaceScopedById(processoId)
                .or(() -> processoRepository.findById(processoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "institutional.workbench.workspace.read", maxMillis = 1800, critical = false)
    public InstitutionalWorkbenchWorkspaceResponse workspace() {
        Usuario usuario = currentUserService.getRequired();
        InstitutionalWorkbenchProfileResponse profile = profileResolver.resolve(usuario);
        InstitutionalWorkbenchQuickActionsResponse quickActions = projectionService.quickActions(null);
        InstitutionalWorkbenchOperationalQueueResponse operationalQueue = projectionService.operationalQueue(12);
        InstitutionalWorkbenchWidgetPolicyEngine.Projection projection = widgetPolicyEngine.project(profile, quickActions, operationalQueue);
        ArrayList<String> warnings = new ArrayList<>();
        warnings.addAll(quickActions.warnings());
        warnings.addAll(operationalQueue.warnings());
        if (projection.routes().isEmpty()) {
            warnings.add("Nenhuma rota operacional ativa foi projetada para o perfil atual.");
        }
        return new InstitutionalWorkbenchWorkspaceResponse(
                Instant.now(),
                profile,
                projection.metrics(),
                projection.widgets(),
                projection.routes(),
                quickActions,
                operationalQueue,
                List.copyOf(new LinkedHashSet<>(warnings))
        );
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "institutional.workbench.action-preview-envelope.read", maxMillis = 1500, critical = false)
    public InstitutionalWorkbenchActionPreviewResponse actionPreview(Long processoId, String actionCode) {
        Processo processo = loadScopedProcess(processoId);
        InstitutionalWorkbenchActionResponse action = projectionService.previewAction(processo, actionCode);
        InstitutionalWorkbenchExplainabilityResponse explainability = projectionService.previewExplainability(processo, actionCode);
        ArrayList<String> warnings = new ArrayList<>(action.warnings());
        if (processo == null) {
            warnings.add("Preview gerado sem processo vinculado; a decisão usa a malha institucional catalogada do ato.");
        }
        return new InstitutionalWorkbenchActionPreviewResponse(
                Instant.now(),
                projectionService.currentActorClass(),
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                action,
                explainability,
                List.copyOf(new LinkedHashSet<>(warnings))
        );
    }
}
