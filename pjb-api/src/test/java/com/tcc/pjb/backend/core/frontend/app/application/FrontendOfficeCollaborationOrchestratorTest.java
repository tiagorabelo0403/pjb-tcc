package com.tcc.pjb.backend.core.frontend.app.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationDecisionResultView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeAffiliationInviteView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferPreviewView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferView;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationFinalApprovalRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeAffiliationInviteRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferDecisionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeProcessTransferRequest;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeAffiliationInviteService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessTransferService;
import java.util.List;
import org.junit.jupiter.api.Test;

class FrontendOfficeCollaborationOrchestratorTest {

    private final OfficeAffiliationInviteService inviteService = mock(OfficeAffiliationInviteService.class);
    private final OfficeProcessTransferService transferService = mock(OfficeProcessTransferService.class);
    private final FrontendOfficeCollaborationOrchestrator orchestrator = new FrontendOfficeCollaborationOrchestrator(inviteService, transferService);

    @Test
    void inviteMetodosDelegam() {
        var incoming = List.of(mock(PjbFrontendOfficeAffiliationInviteView.class));
        var byOffice = List.of(mock(PjbFrontendOfficeAffiliationInviteView.class), mock(PjbFrontendOfficeAffiliationInviteView.class));
        var created = mock(PjbFrontendOfficeAffiliationInviteView.class);
        var accepted = mock(PjbFrontendOfficeAffiliationDecisionResultView.class);
        var activated = mock(PjbFrontendOfficeAffiliationDecisionResultView.class);
        var rejected = mock(PjbFrontendOfficeAffiliationInviteView.class);
        var revoked = mock(PjbFrontendOfficeAffiliationInviteView.class);
        var createReq = mock(FrontendOfficeAffiliationInviteRequest.class);
        var decisionReq = mock(FrontendOfficeAffiliationDecisionRequest.class);
        var finalApprovalReq = mock(FrontendOfficeAffiliationFinalApprovalRequest.class);

        when(inviteService.myIncomingInvites()).thenReturn(incoming);
        when(inviteService.officeInvites(7L)).thenReturn(byOffice);
        when(inviteService.createInvite(createReq)).thenReturn(created);
        when(inviteService.acceptInvite(1L, decisionReq)).thenReturn(accepted);
        when(inviteService.confirmInviteActivation(2L, finalApprovalReq)).thenReturn(activated);
        when(inviteService.rejectInvite(3L)).thenReturn(rejected);
        when(inviteService.revokeInvite(4L)).thenReturn(revoked);

        assertThat(orchestrator.myIncomingInvites()).isSameAs(incoming);
        assertThat(orchestrator.officeInvites(7L)).isSameAs(byOffice);
        assertThat(orchestrator.createInvite(createReq)).isSameAs(created);
        assertThat(orchestrator.acceptInvite(1L, decisionReq)).isSameAs(accepted);
        assertThat(orchestrator.confirmInviteActivation(2L, finalApprovalReq)).isSameAs(activated);
        assertThat(orchestrator.rejectInvite(3L)).isSameAs(rejected);
        assertThat(orchestrator.revokeInvite(4L)).isSameAs(revoked);
    }

    @Test
    void transferMetodosDelegam() {
        var incoming = List.of(mock(PjbFrontendOfficeProcessTransferView.class));
        var preview = mock(PjbFrontendOfficeProcessTransferPreviewView.class);
        var byOffice = List.of(mock(PjbFrontendOfficeProcessTransferView.class));
        var created = mock(PjbFrontendOfficeProcessTransferView.class);
        var accepted = mock(PjbFrontendOfficeProcessTransferView.class);
        var rejected = mock(PjbFrontendOfficeProcessTransferView.class);
        var createReq = mock(FrontendOfficeProcessTransferRequest.class);
        var decisionReq = mock(FrontendOfficeProcessTransferDecisionRequest.class);

        when(transferService.myIncomingTransfers()).thenReturn(incoming);
        when(transferService.previewTransfer(createReq)).thenReturn(preview);
        when(transferService.officeTransfers(9L)).thenReturn(byOffice);
        when(transferService.createTransfer(createReq)).thenReturn(created);
        when(transferService.acceptTransfer(11L, decisionReq)).thenReturn(accepted);
        when(transferService.rejectTransfer(12L)).thenReturn(rejected);

        assertThat(orchestrator.myIncomingTransfers()).isSameAs(incoming);
        assertThat(orchestrator.previewTransfer(createReq)).isSameAs(preview);
        assertThat(orchestrator.officeTransfers(9L)).isSameAs(byOffice);
        assertThat(orchestrator.createTransfer(createReq)).isSameAs(created);
        assertThat(orchestrator.acceptTransfer(11L, decisionReq)).isSameAs(accepted);
        assertThat(orchestrator.rejectTransfer(12L)).isSameAs(rejected);
    }
}
