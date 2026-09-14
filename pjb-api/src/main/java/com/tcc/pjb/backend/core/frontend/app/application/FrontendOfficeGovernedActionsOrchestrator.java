package com.tcc.pjb.backend.core.frontend.app.application;

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
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de PjbFrontendAppApplicationService: ações governadas do escritório
 * sobre processos -- escopo do workspace, filing de documentos, petição, protocolo
 * externo, upload governado (batch/reserve/direct/finalize) e workspace multimídia.
 */
@Service
public class FrontendOfficeGovernedActionsOrchestrator {

    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final OfficeGovernedDocumentFilingService officeGovernedDocumentFilingService;
    private final OfficeGovernedPetitionService officeGovernedPetitionService;
    private final OfficeGovernedExternalProtocolService officeGovernedExternalProtocolService;
    private final OfficeGovernedUploadIngressService officeGovernedUploadIngressService;
    private final OfficeGovernedMultimediaWorkspaceService officeGovernedMultimediaWorkspaceService;

    public FrontendOfficeGovernedActionsOrchestrator(OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                                     OfficeGovernedDocumentFilingService officeGovernedDocumentFilingService,
                                                     OfficeGovernedPetitionService officeGovernedPetitionService,
                                                     OfficeGovernedExternalProtocolService officeGovernedExternalProtocolService,
                                                     OfficeGovernedUploadIngressService officeGovernedUploadIngressService,
                                                     OfficeGovernedMultimediaWorkspaceService officeGovernedMultimediaWorkspaceService) {
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.officeGovernedDocumentFilingService = Objects.requireNonNull(officeGovernedDocumentFilingService);
        this.officeGovernedPetitionService = Objects.requireNonNull(officeGovernedPetitionService);
        this.officeGovernedExternalProtocolService = Objects.requireNonNull(officeGovernedExternalProtocolService);
        this.officeGovernedUploadIngressService = Objects.requireNonNull(officeGovernedUploadIngressService);
        this.officeGovernedMultimediaWorkspaceService = Objects.requireNonNull(officeGovernedMultimediaWorkspaceService);
    }

    public PjbFrontendOfficeWorkspaceProcessPageView currentWorkspaceProcesses(FrontendOfficeWorkspaceProcessQueryRequest request, HttpServletRequest httpServletRequest) {
        return officeProcessWorkspaceScopeService.currentWorkspaceProcesses(request, httpServletRequest);
    }

    public PjbFrontendOfficeProcessAccessView access(Long processoId, OfficeActionType actionType, HttpServletRequest httpServletRequest) {
        return officeProcessWorkspaceScopeService.access(processoId, actionType, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedPetitionView submitPetition(Long processoId, FrontendOfficeGovernedPetitionRequest request, HttpServletRequest httpServletRequest) {
        return officeGovernedPetitionService.submit(processoId, request, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedDocumentBatchPreviewView previewDocumentBatch(Long processoId, UUID batchId, HttpServletRequest httpServletRequest) {
        return officeGovernedDocumentFilingService.preview(processoId, batchId, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedDocumentBatchLinkView linkDocumentBatch(Long processoId, FrontendOfficeGovernedDocumentBatchLinkRequest request, HttpServletRequest httpServletRequest) {
        return officeGovernedDocumentFilingService.linkBatch(processoId, request, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedProtocolSubmissionView submitProtocol(Long processoId, Long protocolPackageId, FrontendOfficeGovernedProtocolSubmitRequest request, HttpServletRequest httpServletRequest) {
        return officeGovernedExternalProtocolService.submit(processoId, protocolPackageId, request, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedUploadBatchView createUploadBatch(Long processoId, FrontendOfficeGovernedUploadBatchCreateRequest request, HttpServletRequest httpServletRequest) {
        return officeGovernedUploadIngressService.createBatch(processoId, request, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedUploadBatchView uploadBatch(Long processoId, UUID batchId, HttpServletRequest httpServletRequest) {
        return officeGovernedUploadIngressService.batch(processoId, batchId, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedUploadItemReservationView reserveUploadItem(Long processoId, UUID batchId, FrontendOfficeGovernedUploadReserveItemRequest request, HttpServletRequest httpServletRequest) {
        return officeGovernedUploadIngressService.reserveItem(processoId, batchId, request, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedUploadIngressView directUpload(Long processoId, UUID batchId, UUID itemId, String token, HttpServletRequest httpServletRequest) throws Exception {
        return officeGovernedUploadIngressService.directUpload(processoId, batchId, itemId, token, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedUploadFinalizeView finalizeUploadBatch(Long processoId, UUID batchId, FrontendOfficeGovernedUploadFinalizeRequest request, HttpServletRequest httpServletRequest) {
        return officeGovernedUploadIngressService.finalizeBatch(processoId, batchId, request, httpServletRequest);
    }

    public PjbFrontendOfficeGovernedMultimediaWorkspaceView previewMultimediaWorkspace(Long processoId, FrontendOfficeGovernedMultimediaWorkspaceRequest request, HttpServletRequest httpServletRequest) {
        return officeGovernedMultimediaWorkspaceService.preview(processoId, request, httpServletRequest);
    }
}
