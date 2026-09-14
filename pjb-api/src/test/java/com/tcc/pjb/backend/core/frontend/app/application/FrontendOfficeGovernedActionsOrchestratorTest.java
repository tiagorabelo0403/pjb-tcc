package com.tcc.pjb.backend.core.frontend.app.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedDocumentBatchLinkView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedDocumentBatchPreviewView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedMultimediaWorkspaceView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedPetitionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedProtocolSubmissionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadBatchView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadFinalizeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadIngressView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedUploadItemReservationView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessPageView;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedDocumentBatchLinkRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedMultimediaWorkspaceRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedPetitionRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedProtocolSubmitRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadBatchCreateRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadFinalizeRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedUploadReserveItemRequest;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceProcessQueryRequest;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedDocumentFilingService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedExternalProtocolService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedMultimediaWorkspaceService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedPetitionService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedUploadIngressService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class FrontendOfficeGovernedActionsOrchestratorTest {

    private final OfficeProcessWorkspaceScopeService workspaceScope = mock(OfficeProcessWorkspaceScopeService.class);
    private final OfficeGovernedDocumentFilingService filing = mock(OfficeGovernedDocumentFilingService.class);
    private final OfficeGovernedPetitionService petition = mock(OfficeGovernedPetitionService.class);
    private final OfficeGovernedExternalProtocolService protocol = mock(OfficeGovernedExternalProtocolService.class);
    private final OfficeGovernedUploadIngressService uploadIngress = mock(OfficeGovernedUploadIngressService.class);
    private final OfficeGovernedMultimediaWorkspaceService multimedia = mock(OfficeGovernedMultimediaWorkspaceService.class);
    private final FrontendOfficeGovernedActionsOrchestrator orchestrator = new FrontendOfficeGovernedActionsOrchestrator(
            workspaceScope, filing, petition, protocol, uploadIngress, multimedia);

    @Test
    void workspaceScopeEPetitionDelegam() {
        var request = new MockHttpServletRequest();
        var queryReq = mock(FrontendOfficeWorkspaceProcessQueryRequest.class);
        var petitionReq = mock(FrontendOfficeGovernedPetitionRequest.class);
        var page = mock(PjbFrontendOfficeWorkspaceProcessPageView.class);
        var access = mock(PjbFrontendOfficeProcessAccessView.class);
        var petitionView = mock(PjbFrontendOfficeGovernedPetitionView.class);
        when(workspaceScope.currentWorkspaceProcesses(queryReq, request)).thenReturn(page);
        when(workspaceScope.access(1L, OfficeActionType.PETICIONAR, request)).thenReturn(access);
        when(petition.submit(1L, petitionReq, request)).thenReturn(petitionView);

        assertThat(orchestrator.currentWorkspaceProcesses(queryReq, request)).isSameAs(page);
        assertThat(orchestrator.access(1L, OfficeActionType.PETICIONAR, request)).isSameAs(access);
        assertThat(orchestrator.submitPetition(1L, petitionReq, request)).isSameAs(petitionView);
    }

    @Test
    void filingEProtocolDelegam() {
        var request = new MockHttpServletRequest();
        var batchId = UUID.randomUUID();
        var linkReq = mock(FrontendOfficeGovernedDocumentBatchLinkRequest.class);
        var protocolReq = mock(FrontendOfficeGovernedProtocolSubmitRequest.class);
        var preview = mock(PjbFrontendOfficeGovernedDocumentBatchPreviewView.class);
        var link = mock(PjbFrontendOfficeGovernedDocumentBatchLinkView.class);
        var protocolView = mock(PjbFrontendOfficeGovernedProtocolSubmissionView.class);
        when(filing.preview(1L, batchId, request)).thenReturn(preview);
        when(filing.linkBatch(1L, linkReq, request)).thenReturn(link);
        when(protocol.submit(1L, 5L, protocolReq, request)).thenReturn(protocolView);

        assertThat(orchestrator.previewDocumentBatch(1L, batchId, request)).isSameAs(preview);
        assertThat(orchestrator.linkDocumentBatch(1L, linkReq, request)).isSameAs(link);
        assertThat(orchestrator.submitProtocol(1L, 5L, protocolReq, request)).isSameAs(protocolView);
    }

    @Test
    void uploadIngressDelega5Metodos() throws Exception {
        var request = new MockHttpServletRequest();
        var batchId = UUID.randomUUID();
        var itemId = UUID.randomUUID();
        var createReq = mock(FrontendOfficeGovernedUploadBatchCreateRequest.class);
        var reserveReq = mock(FrontendOfficeGovernedUploadReserveItemRequest.class);
        var finalizeReq = mock(FrontendOfficeGovernedUploadFinalizeRequest.class);
        var batch = mock(PjbFrontendOfficeGovernedUploadBatchView.class);
        var loadedBatch = mock(PjbFrontendOfficeGovernedUploadBatchView.class);
        var reservation = mock(PjbFrontendOfficeGovernedUploadItemReservationView.class);
        var ingress = mock(PjbFrontendOfficeGovernedUploadIngressView.class);
        var finalized = mock(PjbFrontendOfficeGovernedUploadFinalizeView.class);
        when(uploadIngress.createBatch(1L, createReq, request)).thenReturn(batch);
        when(uploadIngress.batch(1L, batchId, request)).thenReturn(loadedBatch);
        when(uploadIngress.reserveItem(1L, batchId, reserveReq, request)).thenReturn(reservation);
        when(uploadIngress.directUpload(1L, batchId, itemId, "tok", request)).thenReturn(ingress);
        when(uploadIngress.finalizeBatch(1L, batchId, finalizeReq, request)).thenReturn(finalized);

        assertThat(orchestrator.createUploadBatch(1L, createReq, request)).isSameAs(batch);
        assertThat(orchestrator.uploadBatch(1L, batchId, request)).isSameAs(loadedBatch);
        assertThat(orchestrator.reserveUploadItem(1L, batchId, reserveReq, request)).isSameAs(reservation);
        assertThat(orchestrator.directUpload(1L, batchId, itemId, "tok", request)).isSameAs(ingress);
        assertThat(orchestrator.finalizeUploadBatch(1L, batchId, finalizeReq, request)).isSameAs(finalized);
    }

    @Test
    void multimediaDelega() {
        var request = new MockHttpServletRequest();
        var multimediaReq = mock(FrontendOfficeGovernedMultimediaWorkspaceRequest.class);
        var view = mock(PjbFrontendOfficeGovernedMultimediaWorkspaceView.class);
        when(multimedia.preview(1L, multimediaReq, request)).thenReturn(view);
        assertThat(orchestrator.previewMultimediaWorkspace(1L, multimediaReq, request)).isSameAs(view);
    }
}
