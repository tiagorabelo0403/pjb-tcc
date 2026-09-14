package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalManagedCredentialApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalRootAdministratorApprovalApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalStrongSignaturePolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalManagedCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalRootAdministratorApproval;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStrongSignaturePolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalIntegrationCredentialApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.InstitutionalRootAdminApprovalDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalRootAdministratorApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStrongSignaturePolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSimpleFundamentosRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalCredentialsGovernanceSurfaceServiceTest {

    private final InstitutionalManagedCredentialApplicationService managedCredentialApplicationService = mock(InstitutionalManagedCredentialApplicationService.class);
    private final InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService = mock(InstitutionalRootAdministratorApprovalApplicationService.class);
    private final InstitutionalStrongSignaturePolicyApplicationService strongSignaturePolicyApplicationService = mock(InstitutionalStrongSignaturePolicyApplicationService.class);
    private final InstitutionalIntegrationCredentialApplicationService integrationCredentialApplicationService = mock(InstitutionalIntegrationCredentialApplicationService.class);
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport = mock(NationalCommunicationInstitutionalGovernanceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalCredentialsGovernanceSurfaceService service = new NationalCommunicationInstitutionalCredentialsGovernanceSurfaceService(
            managedCredentialApplicationService, rootAdministratorApprovalApplicationService,
            strongSignaturePolicyApplicationService, integrationCredentialApplicationService, governanceAssemblerSupport);

    @Test
    void emitirCredencialGerenciadaPassaTodos8CamposDoRequest() {
        var request = new NationalCommunicationInstitutionalManagedCredentialIssueRequest(
                "nom-1", 42L, "display", "lane", List.of("10.0.0.0/8"), 30, List.of("fund"));
        var credential = mock(InstitutionalManagedCredential.class);
        var response = mock(NationalCommunicationInstitutionalManagedCredentialResponse.class);
        when(managedCredentialApplicationService.emitir("aff-1", "nom-1", 42L, "display", "lane", List.of("10.0.0.0/8"), 30, List.of("fund"))).thenReturn(credential);
        when(governanceAssemblerSupport.toResponse(credential)).thenReturn(response);

        assertThat(service.emitirCredencialGerenciada("aff-1", request)).isSameAs(response);
    }

    @Test
    void emitirCredencialGerenciadaRequestNuloPassaNullsEListasVazias() {
        var credential = mock(InstitutionalManagedCredential.class);
        var response = mock(NationalCommunicationInstitutionalManagedCredentialResponse.class);
        when(managedCredentialApplicationService.emitir("aff-2", null, null, null, null, List.of(), null, List.of())).thenReturn(credential);
        when(governanceAssemblerSupport.toResponse(credential)).thenReturn(response);

        assertThat(service.emitirCredencialGerenciada("aff-2", null)).isSameAs(response);
    }

    @Test
    void decidirAprovacaoAdministradorRaizPassaTodosOsCamposDoRequest() {
        var request = new InstitutionalRootAdminApprovalDecisionRequest(99L, "user-name", "SOURCE", true, List.of("fund"));
        var approval = mock(InstitutionalRootAdministratorApproval.class);
        var response = mock(NationalCommunicationInstitutionalRootAdministratorApprovalResponse.class);
        when(rootAdministratorApprovalApplicationService.decidir("aff-3", 99L, "user-name", "SOURCE", true, List.of("fund"))).thenReturn(approval);
        when(governanceAssemblerSupport.toResponse(approval)).thenReturn(response);

        assertThat(service.decidirAprovacaoAdministradorRaiz("aff-3", request)).isSameAs(response);
    }

    @Test
    void assinaturaForteDelegaComAffiliationENominationId() {
        var policy = mock(InstitutionalStrongSignaturePolicy.class);
        var response = mock(NationalCommunicationInstitutionalStrongSignaturePolicyResponse.class);
        when(strongSignaturePolicyApplicationService.avaliar("aff-4", "nom-1")).thenReturn(policy);
        when(governanceAssemblerSupport.toResponse(policy)).thenReturn(response);

        assertThat(service.assinaturaForte("aff-4", "nom-1")).isSameAs(response);
    }

    @Test
    void emitirCredencialIntegracaoPassaTodos5CamposDoRequest() {
        var request = new NationalCommunicationInstitutionalIntegrationCredentialIssueRequest("aff-5", "display", List.of("MNI"), List.of("origem-1"), List.of("fund"));
        var issued = mock(InstitutionalIntegrationCredentialApplicationService.IssuedCredential.class);
        var response = mock(NationalCommunicationInstitutionalIntegrationCredentialResponse.class);
        when(integrationCredentialApplicationService.issue("aff-5", "display", List.of("MNI"), List.of("origem-1"), List.of("fund"))).thenReturn(issued);
        when(governanceAssemblerSupport.toResponse(issued)).thenReturn(response);

        assertThat(service.emitirCredencial(request)).isSameAs(response);
    }

    @Test
    void revogarCredencialGerenciadaRequestNuloUsaListaVazia() {
        var credential = mock(InstitutionalManagedCredential.class);
        var response = mock(NationalCommunicationInstitutionalManagedCredentialResponse.class);
        when(managedCredentialApplicationService.revogar("cred-1", List.of())).thenReturn(credential);
        when(governanceAssemblerSupport.toResponse(credential)).thenReturn(response);

        assertThat(service.revogarCredencialGerenciada("aff-x", "cred-1", null)).isSameAs(response);
    }
}
