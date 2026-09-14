package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalManagedCredentialApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalRootAdministratorApprovalApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalStrongSignaturePolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalManagedCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalRootAdministratorApproval;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalIntegrationCredentialApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalRootAdministratorApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStrongSignaturePolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.InstitutionalRootAdminApprovalDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSimpleFundamentosRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCallTrailCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCallTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalGovernanceSurfaceFacadeService:
 * governança de credenciais -- managed credentials, aprovação de administrador raiz,
 * política de assinatura forte, credenciais de integração (emissão/rotação/revogação/trilha).
 */
@Service
public class NationalCommunicationInstitutionalCredentialsGovernanceSurfaceService {

    private final InstitutionalManagedCredentialApplicationService managedCredentialApplicationService;
    private final InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService;
    private final InstitutionalStrongSignaturePolicyApplicationService strongSignaturePolicyApplicationService;
    private final InstitutionalIntegrationCredentialApplicationService integrationCredentialApplicationService;
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport;

    public NationalCommunicationInstitutionalCredentialsGovernanceSurfaceService(
            InstitutionalManagedCredentialApplicationService managedCredentialApplicationService,
            InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService,
            InstitutionalStrongSignaturePolicyApplicationService strongSignaturePolicyApplicationService,
            InstitutionalIntegrationCredentialApplicationService integrationCredentialApplicationService,
            NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport) {
        this.managedCredentialApplicationService = managedCredentialApplicationService;
        this.rootAdministratorApprovalApplicationService = rootAdministratorApprovalApplicationService;
        this.strongSignaturePolicyApplicationService = strongSignaturePolicyApplicationService;
        this.integrationCredentialApplicationService = integrationCredentialApplicationService;
        this.governanceAssemblerSupport = governanceAssemblerSupport;
    }

    public List<NationalCommunicationInstitutionalManagedCredentialResponse> credenciaisGerenciadas(String affiliationId) {
        return managedCredentialApplicationService.listar(affiliationId).stream().map(governanceAssemblerSupport::toResponse).toList();
    }

    public NationalCommunicationInstitutionalManagedCredentialResponse emitirCredencialGerenciada(String affiliationId,
                                                                                                  NationalCommunicationInstitutionalManagedCredentialIssueRequest request) {
        InstitutionalManagedCredential issued = managedCredentialApplicationService.emitir(
                affiliationId,
                request == null ? null : request.nominationId(),
                request == null ? null : request.nominatedUserId(),
                request == null ? null : request.displayName(),
                request == null ? null : request.laneCode(),
                request == null ? List.of() : request.allowedNetworks(),
                request == null ? null : request.rotationWindowDays(),
                request == null ? List.of() : request.fundamentos());
        return governanceAssemblerSupport.toResponse(issued);
    }

    public NationalCommunicationInstitutionalManagedCredentialResponse revogarCredencialGerenciada(String affiliationId,
                                                                                                   String credentialId,
                                                                                                   NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return governanceAssemblerSupport.toResponse(managedCredentialApplicationService.revogar(
                credentialId,
                request == null ? List.of() : request.fundamentos()));
    }

    public NationalCommunicationInstitutionalRootAdministratorApprovalResponse aprovacaoAdministradorRaiz(String affiliationId) {
        return governanceAssemblerSupport.toResponse(rootAdministratorApprovalApplicationService.consolidar(affiliationId));
    }

    public NationalCommunicationInstitutionalRootAdministratorApprovalResponse decidirAprovacaoAdministradorRaiz(String affiliationId,
                                                                                                                InstitutionalRootAdminApprovalDecisionRequest request) {
        InstitutionalRootAdministratorApproval approval = rootAdministratorApprovalApplicationService.decidir(
                affiliationId,
                request == null ? null : request.candidateUserId(),
                request == null ? null : request.candidateUserName(),
                request == null ? null : request.approvalSource(),
                request != null && request.approved(),
                request == null ? List.of() : request.fundamentos());
        return governanceAssemblerSupport.toResponse(approval);
    }

    public NationalCommunicationInstitutionalStrongSignaturePolicyResponse assinaturaForte(String affiliationId, String nominationId) {
        return governanceAssemblerSupport.toResponse(strongSignaturePolicyApplicationService.avaliar(affiliationId, nominationId));
    }

    public NationalCommunicationInstitutionalIntegrationCredentialResponse emitirCredencial(NationalCommunicationInstitutionalIntegrationCredentialIssueRequest request) {
        InstitutionalIntegrationCredentialApplicationService.IssuedCredential issued = integrationCredentialApplicationService.issue(
                request.affiliationId(),
                request.displayName(),
                request.integrationFamilies(),
                request.originAllowlist(),
                request.fundamentos());
        return governanceAssemblerSupport.toResponse(issued);
    }

    public NationalCommunicationInstitutionalIntegrationCredentialResponse rotacionarCredencial(String credentialId, NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        InstitutionalIntegrationCredentialApplicationService.IssuedCredential issued = integrationCredentialApplicationService.rotate(
                credentialId,
                request == null ? List.of() : request.fundamentos());
        return governanceAssemblerSupport.toResponse(issued);
    }

    public NationalCommunicationInstitutionalIntegrationCredentialResponse revogarCredencial(String credentialId, NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return governanceAssemblerSupport.toResponse(integrationCredentialApplicationService.revoke(
                credentialId,
                request == null ? List.of() : request.fundamentos()), null);
    }

    public List<NationalCommunicationInstitutionalIntegrationCredentialResponse> listarCredenciais(String affiliationId) {
        return integrationCredentialApplicationService.list(affiliationId).stream().map(item -> governanceAssemblerSupport.toResponse(item, null)).toList();
    }

    public NationalCommunicationInstitutionalIntegrationCallTrailResponse registrarChamada(String credentialId, NationalCommunicationInstitutionalIntegrationCallTrailCreateRequest request) {
        return governanceAssemblerSupport.toResponse(integrationCredentialApplicationService.registerCall(
                credentialId,
                request.correlationId(),
                request.origin(),
                request.payloadDigest(),
                request.payloadSignaturePresent() != null && request.payloadSignaturePresent(),
                request.idempotencyKey(),
                request.resultCode(),
                request.findings()));
    }

    public List<NationalCommunicationInstitutionalIntegrationCallTrailResponse> trilhaChamadas(String credentialId) {
        return integrationCredentialApplicationService.trails(credentialId).stream().map(governanceAssemblerSupport::toResponse).toList();
    }
}
