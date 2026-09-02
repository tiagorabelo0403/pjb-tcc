package com.tcc.pjb.backend.core.frontend.app.application;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalOrganExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalRoleExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.service.professional.ProfessionalForensicExecutiveDashboardService;
import com.tcc.pjb.backend.service.professional.ProfessionalOrganExecutiveDashboardService;
import com.tcc.pjb.backend.service.professional.ProfessionalRoleExecutiveDashboardService;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de PjbFrontendAppApplicationService: dashboards executivos por
 * classificação profissional -- forense (transversal), por papel (magistratura,
 * defensoria, procuradoria) e por órgão institucional (mesmas 3 categorias).
 */
@Service
public class FrontendProfessionalDashboardOrchestrator {

    private final ProfessionalForensicExecutiveDashboardService professionalForensicExecutiveDashboardService;
    private final ProfessionalRoleExecutiveDashboardService professionalRoleExecutiveDashboardService;
    private final ProfessionalOrganExecutiveDashboardService professionalOrganExecutiveDashboardService;

    public FrontendProfessionalDashboardOrchestrator(ProfessionalForensicExecutiveDashboardService professionalForensicExecutiveDashboardService,
                                                     ProfessionalRoleExecutiveDashboardService professionalRoleExecutiveDashboardService,
                                                     ProfessionalOrganExecutiveDashboardService professionalOrganExecutiveDashboardService) {
        this.professionalForensicExecutiveDashboardService = Objects.requireNonNull(professionalForensicExecutiveDashboardService);
        this.professionalRoleExecutiveDashboardService = Objects.requireNonNull(professionalRoleExecutiveDashboardService);
        this.professionalOrganExecutiveDashboardService = Objects.requireNonNull(professionalOrganExecutiveDashboardService);
    }

    public PjbFrontendProfessionalWorkspaceExecutiveDashboardView forensicDashboard(Authentication authentication, LocalDate from, LocalDate to) {
        return professionalForensicExecutiveDashboardService.dashboard(authentication, from, to);
    }

    public PjbFrontendProfessionalRoleExecutiveDashboardView roleMagistratureDashboard(Authentication authentication, LocalDate from, LocalDate to) {
        return professionalRoleExecutiveDashboardService.magistratureDashboard(authentication, from, to);
    }

    public PjbFrontendProfessionalRoleExecutiveDashboardView roleDefensoriaDashboard(Authentication authentication, LocalDate from, LocalDate to) {
        return professionalRoleExecutiveDashboardService.defensoriaDashboard(authentication, from, to);
    }

    public PjbFrontendProfessionalRoleExecutiveDashboardView roleProcuradoriaDashboard(Authentication authentication, LocalDate from, LocalDate to) {
        return professionalRoleExecutiveDashboardService.procuradoriaDashboard(authentication, from, to);
    }

    public PjbFrontendProfessionalOrganExecutiveDashboardView organDashboard(Authentication authentication, LocalDate from, LocalDate to) {
        return professionalOrganExecutiveDashboardService.dashboard(authentication, from, to);
    }

    public PjbFrontendProfessionalOrganExecutiveDashboardView organMagistratureDashboard(Authentication authentication, LocalDate from, LocalDate to) {
        return professionalOrganExecutiveDashboardService.magistratureDashboard(authentication, from, to);
    }

    public PjbFrontendProfessionalOrganExecutiveDashboardView organDefensoriaDashboard(Authentication authentication, LocalDate from, LocalDate to) {
        return professionalOrganExecutiveDashboardService.defensoriaDashboard(authentication, from, to);
    }

    public PjbFrontendProfessionalOrganExecutiveDashboardView organProcuradoriaDashboard(Authentication authentication, LocalDate from, LocalDate to) {
        return professionalOrganExecutiveDashboardService.procuradoriaDashboard(authentication, from, to);
    }
}
