package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalBindingApprovalApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalContextActivationGuardApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalIdentityGuardApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalRepresentativeVerificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalStepUpAuthenticationPolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalTextClosureApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalIdentityGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalRepresentativeVerificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalTextClosureAuditResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBindingApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStepUpPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalContextActivationResponse;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalSurfaceFacadeService, que injetava os
 * 6 colaboradores de guarda de acesso diretamente (38 dependências de construtor).
 */
@Service
public class NationalCommunicationInstitutionalAccessGuardSurfaceService {

    private final InstitutionalRepresentativeVerificationApplicationService representativeVerificationApplicationService;
    private final InstitutionalBindingApprovalApplicationService bindingApprovalApplicationService;
    private final InstitutionalIdentityGuardApplicationService identityGuardApplicationService;
    private final InstitutionalStepUpAuthenticationPolicyApplicationService stepUpAuthenticationPolicyApplicationService;
    private final InstitutionalContextActivationGuardApplicationService contextActivationGuardApplicationService;
    private final InstitutionalTextClosureApplicationService textClosureApplicationService;
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport;

    public NationalCommunicationInstitutionalAccessGuardSurfaceService(
            InstitutionalRepresentativeVerificationApplicationService representativeVerificationApplicationService,
            InstitutionalBindingApprovalApplicationService bindingApprovalApplicationService,
            InstitutionalIdentityGuardApplicationService identityGuardApplicationService,
            InstitutionalStepUpAuthenticationPolicyApplicationService stepUpAuthenticationPolicyApplicationService,
            InstitutionalContextActivationGuardApplicationService contextActivationGuardApplicationService,
            InstitutionalTextClosureApplicationService textClosureApplicationService,
            NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport) {
        this.representativeVerificationApplicationService = representativeVerificationApplicationService;
        this.bindingApprovalApplicationService = bindingApprovalApplicationService;
        this.identityGuardApplicationService = identityGuardApplicationService;
        this.stepUpAuthenticationPolicyApplicationService = stepUpAuthenticationPolicyApplicationService;
        this.contextActivationGuardApplicationService = contextActivationGuardApplicationService;
        this.textClosureApplicationService = textClosureApplicationService;
        this.surfaceAssemblerSupport = surfaceAssemblerSupport;
    }

    public Optional<NationalCommunicationInstitutionalRepresentativeVerificationResponse> verificarRepresentante(String requestId) {
        return representativeVerificationApplicationService.buscarSeExistir(requestId).map(surfaceAssemblerSupport::toResponse);
    }

    public NationalCommunicationInstitutionalBindingApprovalResponse aprovacaoVinculo(String affiliationId, String nominationId) {
        return surfaceAssemblerSupport.toResponse(bindingApprovalApplicationService.avaliarAtual(affiliationId, nominationId));
    }

    public NationalCommunicationInstitutionalIdentityGuardResponse guardaIdentidade() {
        return surfaceAssemblerSupport.toResponse(identityGuardApplicationService.avaliarAtual());
    }

    public NationalCommunicationInstitutionalStepUpPolicyResponse politicaStepUp(String affiliationId, String nominationId, String sensitiveAct) {
        return surfaceAssemblerSupport.toResponse(stepUpAuthenticationPolicyApplicationService.avaliarAtual(affiliationId, nominationId, sensitiveAct));
    }

    public NationalCommunicationInstitutionalContextActivationResponse ativacaoContexto(String affiliationId, String nominationId, String unidadeCodigo, String caixaCodigo, String sensitiveAct) {
        return surfaceAssemblerSupport.toResponse(contextActivationGuardApplicationService.avaliarAtual(affiliationId, nominationId, unidadeCodigo, caixaCodigo, sensitiveAct));
    }

    public NationalCommunicationInstitutionalTextClosureAuditResponse fechamentoTexto() {
        return surfaceAssemblerSupport.toResponse(textClosureApplicationService.auditar());
    }
}
