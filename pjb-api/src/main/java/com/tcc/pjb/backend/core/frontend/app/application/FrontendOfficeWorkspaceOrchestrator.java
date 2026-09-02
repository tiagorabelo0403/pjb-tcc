package com.tcc.pjb.backend.core.frontend.app.application;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOwnedOfficeView;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeModeUpdateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceCreateRequest;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceCreationService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceDashboardService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceModeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de PjbFrontendAppApplicationService: ciclo de vida do escritório —
 * modo ativo (personal/oficial), criação de escritórios próprios e sumário do workspace.
 */
@Service
public class FrontendOfficeWorkspaceOrchestrator {

    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final OfficeWorkspaceCreationService officeWorkspaceCreationService;
    private final OfficeWorkspaceDashboardService officeWorkspaceDashboardService;

    public FrontendOfficeWorkspaceOrchestrator(OfficeWorkspaceModeService officeWorkspaceModeService,
                                                OfficeWorkspaceCreationService officeWorkspaceCreationService,
                                                OfficeWorkspaceDashboardService officeWorkspaceDashboardService) {
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.officeWorkspaceCreationService = Objects.requireNonNull(officeWorkspaceCreationService);
        this.officeWorkspaceDashboardService = Objects.requireNonNull(officeWorkspaceDashboardService);
    }

    public PjbFrontendOfficeModeView currentMode(HttpServletRequest request) {
        return officeWorkspaceModeService.current(request);
    }

    public PjbFrontendOfficeModeView updateMode(FrontendOfficeModeUpdateRequest request) {
        return officeWorkspaceModeService.update(request);
    }

    public PjbFrontendOfficeModeView clearMode() {
        return officeWorkspaceModeService.clear();
    }

    public PjbFrontendOfficeModeView createOwnOffice(FrontendOfficeWorkspaceCreateRequest request) {
        return officeWorkspaceCreationService.createOwnOffice(request);
    }

    public PjbFrontendOfficeModeView ensurePersonalOffice() {
        return officeWorkspaceCreationService.ensurePersonalOffice();
    }

    public List<PjbFrontendOwnedOfficeView> myOwnedOffices() {
        return officeWorkspaceCreationService.myOwnedOffices();
    }

    public PjbFrontendOfficeWorkspaceSummaryView currentSummary(HttpServletRequest request, Long equipeId) {
        return officeWorkspaceDashboardService.currentSummary(request, equipeId);
    }
}
