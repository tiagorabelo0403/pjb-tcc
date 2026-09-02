package com.tcc.pjb.backend.core.frontend.app.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOwnedOfficeView;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeModeUpdateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceCreateRequest;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceCreationService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceDashboardService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceModeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class FrontendOfficeWorkspaceOrchestratorTest {

    private final OfficeWorkspaceModeService modeService = mock(OfficeWorkspaceModeService.class);
    private final OfficeWorkspaceCreationService creationService = mock(OfficeWorkspaceCreationService.class);
    private final OfficeWorkspaceDashboardService dashboardService = mock(OfficeWorkspaceDashboardService.class);
    private final FrontendOfficeWorkspaceOrchestrator orchestrator = new FrontendOfficeWorkspaceOrchestrator(modeService, creationService, dashboardService);

    @Test
    void modeMetodosDelegam() {
        var request = new MockHttpServletRequest();
        var expectedCurrent = mock(PjbFrontendOfficeModeView.class);
        var expectedUpdate = mock(PjbFrontendOfficeModeView.class);
        var expectedClear = mock(PjbFrontendOfficeModeView.class);
        var updateReq = mock(FrontendOfficeModeUpdateRequest.class);
        when(modeService.current(request)).thenReturn(expectedCurrent);
        when(modeService.update(updateReq)).thenReturn(expectedUpdate);
        when(modeService.clear()).thenReturn(expectedClear);

        assertThat(orchestrator.currentMode(request)).isSameAs(expectedCurrent);
        assertThat(orchestrator.updateMode(updateReq)).isSameAs(expectedUpdate);
        assertThat(orchestrator.clearMode()).isSameAs(expectedClear);
    }

    @Test
    void creationMetodosDelegam() {
        var expectedOwn = mock(PjbFrontendOfficeModeView.class);
        var expectedPersonal = mock(PjbFrontendOfficeModeView.class);
        var owned = List.of(mock(PjbFrontendOwnedOfficeView.class), mock(PjbFrontendOwnedOfficeView.class));
        var createReq = mock(FrontendOfficeWorkspaceCreateRequest.class);
        when(creationService.createOwnOffice(createReq)).thenReturn(expectedOwn);
        when(creationService.ensurePersonalOffice()).thenReturn(expectedPersonal);
        when(creationService.myOwnedOffices()).thenReturn(owned);

        assertThat(orchestrator.createOwnOffice(createReq)).isSameAs(expectedOwn);
        assertThat(orchestrator.ensurePersonalOffice()).isSameAs(expectedPersonal);
        assertThat(orchestrator.myOwnedOffices()).isSameAs(owned);
    }

    @Test
    void currentSummaryDelegaComRequestEEquipeId() {
        var request = new MockHttpServletRequest();
        var expected = mock(PjbFrontendOfficeWorkspaceSummaryView.class);
        when(dashboardService.currentSummary(request, 42L)).thenReturn(expected);
        assertThat(orchestrator.currentSummary(request, 42L)).isSameAs(expected);
    }
}
