package com.tcc.pjb.backend.core.frontend.app.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessReadingModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceLegalCockpitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceMainDashboardView;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeQueueItemDto;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeSignatureQueueService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceExecutiveDashboardService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceLegalCockpitService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceMainDashboardService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeWorkspaceTeamAvatarService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

class FrontendOfficeExperienceOrchestratorTest {

    private final OfficeSignatureQueueService signatureQueue = mock(OfficeSignatureQueueService.class);
    private final OfficeWorkspaceLegalCockpitService legalCockpit = mock(OfficeWorkspaceLegalCockpitService.class);
    private final OfficeWorkspaceMainDashboardService mainDashboard = mock(OfficeWorkspaceMainDashboardService.class);
    private final OfficeWorkspaceExecutiveDashboardService executiveDashboard = mock(OfficeWorkspaceExecutiveDashboardService.class);
    private final OfficeWorkspaceTeamAvatarService teamAvatar = mock(OfficeWorkspaceTeamAvatarService.class);
    private final FrontendOfficeExperienceOrchestrator orchestrator = new FrontendOfficeExperienceOrchestrator(
            signatureQueue, legalCockpit, mainDashboard, executiveDashboard, teamAvatar);

    @Test
    void signatureQueueDelegaListaAprovarRejeitar() {
        var pageable = PageRequest.of(0, 20);
        @SuppressWarnings("unchecked")
        var expectedPage = (Page<OfficeQueueItemDto>) mock(Page.class);
        var approved = mock(OfficeQueueItemDto.class);
        var rejected = mock(OfficeQueueItemDto.class);
        when(signatureQueue.listarPorSigner(7L, OfficeQueueStatus.PENDING, pageable)).thenReturn(expectedPage);
        when(signatureQueue.aprovar(7L, 100L, "ok")).thenReturn(approved);
        when(signatureQueue.rejeitar(7L, 101L, "no")).thenReturn(rejected);

        assertThat(orchestrator.listSignatureQueue(7L, OfficeQueueStatus.PENDING, pageable)).isSameAs(expectedPage);
        assertThat(orchestrator.approveQueueItem(7L, 100L, "ok")).isSameAs(approved);
        assertThat(orchestrator.rejectQueueItem(7L, 101L, "no")).isSameAs(rejected);
    }

    @Test
    void legalCockpitEReadingModeDelegam() {
        var auth = new TestingAuthenticationToken("u", "n/a");
        var request = new MockHttpServletRequest();
        var from = LocalDate.of(2026, 1, 1);
        var to = LocalDate.of(2026, 12, 31);
        var cockpit = mock(PjbFrontendOfficeWorkspaceLegalCockpitView.class);
        var reading = mock(PjbFrontendOfficeProcessReadingModeView.class);
        when(legalCockpit.cockpit(auth, request, from, to, 42L)).thenReturn(cockpit);
        when(legalCockpit.readingMode(42L, request, from, to)).thenReturn(reading);

        assertThat(orchestrator.legalCockpit(auth, request, from, to, 42L)).isSameAs(cockpit);
        assertThat(orchestrator.readingMode(42L, request, from, to)).isSameAs(reading);
    }

    @Test
    void mainEExecutiveDashboardDelegam() {
        var auth = new TestingAuthenticationToken("u", "n/a");
        var request = new MockHttpServletRequest();
        var from = LocalDate.of(2026, 1, 1);
        var to = LocalDate.of(2026, 12, 31);
        var main = mock(PjbFrontendOfficeWorkspaceMainDashboardView.class);
        var exec = mock(PjbFrontendOfficeWorkspaceExecutiveDashboardView.class);
        when(mainDashboard.dashboard(auth, request, from, to)).thenReturn(main);
        when(executiveDashboard.dashboard(auth, request, from, to)).thenReturn(exec);

        assertThat(orchestrator.mainDashboard(auth, request, from, to)).isSameAs(main);
        assertThat(orchestrator.executiveDashboard(auth, request, from, to)).isSameAs(exec);
    }

    @Test
    void teamAvatarDelega() throws Exception {
        var request = new MockHttpServletRequest();
        var result = mock(OfficeWorkspaceTeamAvatarService.AvatarReadResult.class);
        when(teamAvatar.read(42L, request)).thenReturn(result);
        assertThat(orchestrator.readTeamAvatar(42L, request)).isSameAs(result);
    }
}
