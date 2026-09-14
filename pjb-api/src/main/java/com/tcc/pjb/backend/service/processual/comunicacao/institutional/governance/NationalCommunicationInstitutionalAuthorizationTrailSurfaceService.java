package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationApprovalTrailApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationValidationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRemoteCertificateAuthorizationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSensitiveActAuthorizationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSessionRiskApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationValidationReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalApprovalTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSimpleFundamentosRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.InstitutionalRemoteCertificateAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSessionRiskAssessmentResponse;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalGovernanceSurfaceFacadeService:
 * trilhas de validação/aprovação de adesão, autorização remota de certificado, risco de
 * sessão e autorização de atos sensíveis institucionais.
 */
@Service
public class NationalCommunicationInstitutionalAuthorizationTrailSurfaceService {

    private final InstitutionalAffiliationValidationApplicationService validationApplicationService;
    private final InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService;
    private final InstitutionalRemoteCertificateAuthorizationApplicationService remoteCertificateAuthorizationApplicationService;
    private final InstitutionalSessionRiskApplicationService sessionRiskApplicationService;
    private final InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationApplicationService;
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport;

    public NationalCommunicationInstitutionalAuthorizationTrailSurfaceService(
            InstitutionalAffiliationValidationApplicationService validationApplicationService,
            InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService,
            InstitutionalRemoteCertificateAuthorizationApplicationService remoteCertificateAuthorizationApplicationService,
            InstitutionalSessionRiskApplicationService sessionRiskApplicationService,
            InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationApplicationService,
            NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport) {
        this.validationApplicationService = validationApplicationService;
        this.approvalTrailApplicationService = approvalTrailApplicationService;
        this.remoteCertificateAuthorizationApplicationService = remoteCertificateAuthorizationApplicationService;
        this.sessionRiskApplicationService = sessionRiskApplicationService;
        this.sensitiveActAuthorizationApplicationService = sensitiveActAuthorizationApplicationService;
        this.governanceAssemblerSupport = governanceAssemblerSupport;
    }

    public Optional<NationalCommunicationInstitutionalAffiliationValidationReportResponse> validacaoAdesao(String requestId) {
        return validationApplicationService.buscarUltimo(requestId).map(governanceAssemblerSupport::toResponse);
    }

    public Optional<NationalCommunicationInstitutionalApprovalTrailResponse> trilhaAprovacao(String requestId) {
        return approvalTrailApplicationService.buscarUltima(requestId).map(governanceAssemblerSupport::toResponse);
    }

    public NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse emitirAutorizacaoRemota(InstitutionalRemoteCertificateAuthorizationRequest request) {
        return governanceAssemblerSupport.toResponse(remoteCertificateAuthorizationApplicationService.emitir(
                request.affiliationId(),
                request.nominatedUserId(),
                request.reason(),
                request.allowedNetworks(),
                request.allowedDevices(),
                request.validForHours(),
                request.fundamentos()));
    }

    public NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse revogarAutorizacaoRemota(String authorizationId, NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return governanceAssemblerSupport.toResponse(remoteCertificateAuthorizationApplicationService.revogar(
                authorizationId,
                request == null ? List.of() : request.fundamentos()));
    }

    public List<NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse> listarAutorizacoesRemotas(String affiliationId, Long userId) {
        return remoteCertificateAuthorizationApplicationService.listar(affiliationId, userId).stream().map(governanceAssemblerSupport::toResponse).toList();
    }

    public NationalCommunicationInstitutionalSessionRiskAssessmentResponse riscoSessao(String affiliationId, String nominationId, String unidadeCodigo, String caixaCodigo) {
        return governanceAssemblerSupport.toResponse(sessionRiskApplicationService.avaliarAtual(affiliationId, nominationId, unidadeCodigo, caixaCodigo));
    }

    public NationalCommunicationInstitutionalSensitiveActAuthorizationResponse autorizarAtoSensivel(NationalCommunicationInstitutionalSensitiveActAuthorizationRequest request) {
        InstitutionalSensitiveAct act = InstitutionalSensitiveAct.fromTexto(request.sensitiveAct());
        if (act == null) {
            throw new IllegalArgumentException("Ato sensível institucional não reconhecido.");
        }
        return governanceAssemblerSupport.toResponse(sensitiveActAuthorizationApplicationService.autorizar(act, request.affiliationId(), request.nominationId()));
    }
}
