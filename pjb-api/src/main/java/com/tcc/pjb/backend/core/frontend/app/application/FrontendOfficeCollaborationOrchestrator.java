package com.tcc.pjb.backend.core.frontend.app.application;

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
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraído (F6) de PjbFrontendAppApplicationService: colaboração entre escritórios --
 * convites de afiliação (aceitar/rejeitar/revogar) e transferência de processo entre
 * escritórios (preview/criar/aceitar/rejeitar).
 */
@Service
public class FrontendOfficeCollaborationOrchestrator {

    private final OfficeAffiliationInviteService officeAffiliationInviteService;
    private final OfficeProcessTransferService officeProcessTransferService;

    public FrontendOfficeCollaborationOrchestrator(OfficeAffiliationInviteService officeAffiliationInviteService,
                                                    OfficeProcessTransferService officeProcessTransferService) {
        this.officeAffiliationInviteService = Objects.requireNonNull(officeAffiliationInviteService);
        this.officeProcessTransferService = Objects.requireNonNull(officeProcessTransferService);
    }

    public List<PjbFrontendOfficeAffiliationInviteView> myIncomingInvites() {
        return officeAffiliationInviteService.myIncomingInvites();
    }

    public List<PjbFrontendOfficeAffiliationInviteView> officeInvites(Long equipeId) {
        return officeAffiliationInviteService.officeInvites(equipeId);
    }

    public PjbFrontendOfficeAffiliationInviteView createInvite(FrontendOfficeAffiliationInviteRequest request) {
        return officeAffiliationInviteService.createInvite(request);
    }

    public PjbFrontendOfficeAffiliationDecisionResultView acceptInvite(Long inviteId, FrontendOfficeAffiliationDecisionRequest request) {
        return officeAffiliationInviteService.acceptInvite(inviteId, request);
    }

    public PjbFrontendOfficeAffiliationDecisionResultView confirmInviteActivation(Long inviteId, FrontendOfficeAffiliationFinalApprovalRequest request) {
        return officeAffiliationInviteService.confirmInviteActivation(inviteId, request);
    }

    public PjbFrontendOfficeAffiliationInviteView rejectInvite(Long inviteId) {
        return officeAffiliationInviteService.rejectInvite(inviteId);
    }

    public PjbFrontendOfficeAffiliationInviteView revokeInvite(Long inviteId) {
        return officeAffiliationInviteService.revokeInvite(inviteId);
    }

    public List<PjbFrontendOfficeProcessTransferView> myIncomingTransfers() {
        return officeProcessTransferService.myIncomingTransfers();
    }

    public PjbFrontendOfficeProcessTransferPreviewView previewTransfer(FrontendOfficeProcessTransferRequest request) {
        return officeProcessTransferService.previewTransfer(request);
    }

    public List<PjbFrontendOfficeProcessTransferView> officeTransfers(Long equipeId) {
        return officeProcessTransferService.officeTransfers(equipeId);
    }

    public PjbFrontendOfficeProcessTransferView createTransfer(FrontendOfficeProcessTransferRequest request) {
        return officeProcessTransferService.createTransfer(request);
    }

    public PjbFrontendOfficeProcessTransferView acceptTransfer(Long transferId, FrontendOfficeProcessTransferDecisionRequest request) {
        return officeProcessTransferService.acceptTransfer(transferId, request);
    }

    public PjbFrontendOfficeProcessTransferView rejectTransfer(Long transferId) {
        return officeProcessTransferService.rejectTransfer(transferId);
    }
}
