package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAccessContextMaterializationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAccessContextSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalApiEdgeSecurityProfileApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalIntegrationSecurityPolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRecertificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRevocationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalApiEdgeSecurityProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRevocationResult;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRevocationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRevocationResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalGovernanceSurfaceFacadeService:
 * contexto de acesso materializado, perfil de segurança de edge de API, recertificação
 * periódica, revogação de acessos e política de segurança para integrações.
 */
@Service
public class NationalCommunicationInstitutionalAccessSecuritySurfaceService {

    private final InstitutionalAccessContextMaterializationApplicationService accessContextMaterializationApplicationService;
    private final InstitutionalApiEdgeSecurityProfileApplicationService apiEdgeSecurityProfileApplicationService;
    private final InstitutionalRecertificationApplicationService recertificationApplicationService;
    private final InstitutionalRevocationApplicationService revocationApplicationService;
    private final InstitutionalIntegrationSecurityPolicyApplicationService integrationSecurityPolicyApplicationService;
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport;

    public NationalCommunicationInstitutionalAccessSecuritySurfaceService(
            InstitutionalAccessContextMaterializationApplicationService accessContextMaterializationApplicationService,
            InstitutionalApiEdgeSecurityProfileApplicationService apiEdgeSecurityProfileApplicationService,
            InstitutionalRecertificationApplicationService recertificationApplicationService,
            InstitutionalRevocationApplicationService revocationApplicationService,
            InstitutionalIntegrationSecurityPolicyApplicationService integrationSecurityPolicyApplicationService,
            NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport) {
        this.accessContextMaterializationApplicationService = accessContextMaterializationApplicationService;
        this.apiEdgeSecurityProfileApplicationService = apiEdgeSecurityProfileApplicationService;
        this.recertificationApplicationService = recertificationApplicationService;
        this.revocationApplicationService = revocationApplicationService;
        this.integrationSecurityPolicyApplicationService = integrationSecurityPolicyApplicationService;
        this.governanceAssemblerSupport = governanceAssemblerSupport;
    }

    public NationalCommunicationInstitutionalAccessContextResponse contextoAcesso(String affiliationId, String nominationId) {
        InstitutionalAccessContextSnapshot snapshot = accessContextMaterializationApplicationService.materializar(affiliationId, nominationId);
        return governanceAssemblerSupport.toResponse(snapshot);
    }

    public NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse perfilSegurancaApi(String affiliationId) {
        InstitutionalApiEdgeSecurityProfile profile = apiEdgeSecurityProfileApplicationService.avaliar(affiliationId);
        return governanceAssemblerSupport.toResponse(profile);
    }

    public List<NationalCommunicationInstitutionalRecertificationResponse> recertificacoes(String scope) {
        return recertificationApplicationService.listar(scope).stream().map(governanceAssemblerSupport::toResponse).toList();
    }

    public NationalCommunicationInstitutionalRecertificationResponse recertificar(String affiliationId, NationalCommunicationInstitutionalRecertificationRequest request) {
        return governanceAssemblerSupport.toResponse(recertificationApplicationService.recertificar(
                affiliationId,
                request == null ? List.of() : request.fundamentos()));
    }

    public NationalCommunicationInstitutionalRevocationResponse revogarAcessos(String affiliationId, NationalCommunicationInstitutionalRevocationRequest request) {
        InstitutionalRevocationResult result = revocationApplicationService.revogar(
                affiliationId,
                request == null ? null : request.nominatedUserId(),
                request == null ? null : request.unidadeCodigo(),
                request != null && Boolean.TRUE.equals(request.revogarAfiliacao()),
                request == null ? List.of() : request.fundamentos());
        return governanceAssemblerSupport.toResponse(result);
    }

    public List<NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse> integracoesGovernanca(String scope, String affiliationId) {
        return integrationSecurityPolicyApplicationService.listar(scope, affiliationId).stream().map(governanceAssemblerSupport::toResponse).toList();
    }
}
