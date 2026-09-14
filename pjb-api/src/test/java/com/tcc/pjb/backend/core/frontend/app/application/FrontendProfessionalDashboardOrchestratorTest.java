package com.tcc.pjb.backend.core.frontend.app.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalOrganExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalRoleExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.service.professional.ProfessionalForensicExecutiveDashboardService;
import com.tcc.pjb.backend.service.professional.ProfessionalOrganExecutiveDashboardService;
import com.tcc.pjb.backend.service.professional.ProfessionalRoleExecutiveDashboardService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class FrontendProfessionalDashboardOrchestratorTest {

    private final ProfessionalForensicExecutiveDashboardService forensic = mock(ProfessionalForensicExecutiveDashboardService.class);
    private final ProfessionalRoleExecutiveDashboardService role = mock(ProfessionalRoleExecutiveDashboardService.class);
    private final ProfessionalOrganExecutiveDashboardService organ = mock(ProfessionalOrganExecutiveDashboardService.class);
    private final FrontendProfessionalDashboardOrchestrator orchestrator = new FrontendProfessionalDashboardOrchestrator(forensic, role, organ);

    @Test
    void forensicDashboardDelega() {
        var auth = new TestingAuthenticationToken("u", "n/a");
        var from = LocalDate.of(2026, 1, 1);
        var to = LocalDate.of(2026, 12, 31);
        var expected = mock(PjbFrontendProfessionalWorkspaceExecutiveDashboardView.class);
        when(forensic.dashboard(auth, from, to)).thenReturn(expected);
        assertThat(orchestrator.forensicDashboard(auth, from, to)).isSameAs(expected);
    }

    @Test
    void roleDashboardsDelegam3Metodos() {
        var auth = new TestingAuthenticationToken("u", "n/a");
        var from = LocalDate.of(2026, 1, 1);
        var to = LocalDate.of(2026, 12, 31);
        var mag = mock(PjbFrontendProfessionalRoleExecutiveDashboardView.class);
        var def = mock(PjbFrontendProfessionalRoleExecutiveDashboardView.class);
        var proc = mock(PjbFrontendProfessionalRoleExecutiveDashboardView.class);
        when(role.magistratureDashboard(auth, from, to)).thenReturn(mag);
        when(role.defensoriaDashboard(auth, from, to)).thenReturn(def);
        when(role.procuradoriaDashboard(auth, from, to)).thenReturn(proc);

        assertThat(orchestrator.roleMagistratureDashboard(auth, from, to)).isSameAs(mag);
        assertThat(orchestrator.roleDefensoriaDashboard(auth, from, to)).isSameAs(def);
        assertThat(orchestrator.roleProcuradoriaDashboard(auth, from, to)).isSameAs(proc);
    }

    @Test
    void organDashboardsDelegam4Metodos() {
        var auth = new TestingAuthenticationToken("u", "n/a");
        var from = LocalDate.of(2026, 1, 1);
        var to = LocalDate.of(2026, 12, 31);
        var gen = mock(PjbFrontendProfessionalOrganExecutiveDashboardView.class);
        var mag = mock(PjbFrontendProfessionalOrganExecutiveDashboardView.class);
        var def = mock(PjbFrontendProfessionalOrganExecutiveDashboardView.class);
        var proc = mock(PjbFrontendProfessionalOrganExecutiveDashboardView.class);
        when(organ.dashboard(auth, from, to)).thenReturn(gen);
        when(organ.magistratureDashboard(auth, from, to)).thenReturn(mag);
        when(organ.defensoriaDashboard(auth, from, to)).thenReturn(def);
        when(organ.procuradoriaDashboard(auth, from, to)).thenReturn(proc);

        assertThat(orchestrator.organDashboard(auth, from, to)).isSameAs(gen);
        assertThat(orchestrator.organMagistratureDashboard(auth, from, to)).isSameAs(mag);
        assertThat(orchestrator.organDefensoriaDashboard(auth, from, to)).isSameAs(def);
        assertThat(orchestrator.organProcuradoriaDashboard(auth, from, to)).isSameAs(proc);
    }
}
