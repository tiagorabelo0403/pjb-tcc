package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedMultimediaWorkspaceView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedMultimediaWorkspaceRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeGovernedMultimediaWorkspaceService {

    private final CurrentUserService currentUserService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;

    public OfficeGovernedMultimediaWorkspaceService(CurrentUserService currentUserService,
                                                    OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                                    InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.institutionalMultimediaWorkspaceService = Objects.requireNonNull(institutionalMultimediaWorkspaceService);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeGovernedMultimediaWorkspaceView preview(Long processoId,
                                                                     FrontendOfficeGovernedMultimediaWorkspaceRequest request,
                                                                     HttpServletRequest httpServletRequest) {
        FrontendOfficeGovernedMultimediaWorkspaceRequest safe = request == null
                ? new FrontendOfficeGovernedMultimediaWorkspaceRequest(null, null, null, null, null, null, Map.of())
                : request;
        OfficeActionType action = resolveAction(safe.action());
        PjbFrontendOfficeProcessAccessView access = officeProcessWorkspaceScopeService.access(processoId, action, httpServletRequest);
        Usuario actor = currentUserService.getRequired();
        Map<String, Object> workspace = institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        safe.actorLane(),
                        safe.pieceKind(),
                        processoId,
                        actor.getTipoUsuario(),
                        safe.rawRequest(),
                        Boolean.TRUE.equals(safe.preparingProtocolPackage()),
                        Boolean.TRUE.equals(safe.sigiloSensivel()),
                        Boolean.TRUE.equals(safe.tecnicoPericial())
                )
        );
        return new PjbFrontendOfficeGovernedMultimediaWorkspaceView(
                processoId,
                action.name(),
                normalize(safe.actorLane(), "INSTITUCIONAL"),
                normalize(safe.pieceKind(), "PETICAO_INSTITUCIONAL"),
                access.allowed(),
                access.queueRequired() || containsWarning(access.warnings(), "ASSINATURA_PATRONAL_OBRIGATORIA"),
                access.mode(),
                access.activeEquipeId(),
                access.effectiveSignerUserId(),
                access.effectiveSignerNome(),
                safeList(access.blockers()),
                safeList(access.warnings()),
                Objects.toString(workspace.get("nextAction"), null),
                Objects.toString(workspace.get("pieceProfile"), null),
                Boolean.TRUE.equals(workspace.get("multimediaEnabled")),
                workspace
        );
    }

    private OfficeActionType resolveAction(String raw) {
        if (raw == null || raw.isBlank()) {
            return OfficeActionType.PETICIONAR;
        }
        try {
            return OfficeActionType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return OfficeActionType.PETICIONAR;
        }
    }

    private String normalize(String value, String fallback) {
        String text = value == null ? null : value.trim();
        return text == null || text.isBlank() ? fallback : text;
    }

    private boolean containsWarning(List<String> warnings, String value) {
        return safeList(warnings).stream().anyMatch(value::equalsIgnoreCase);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
