package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalAccessSecuritySurfaceServiceTest {

    private final InstitutionalAccessContextMaterializationApplicationService accessContextMaterializationApplicationService = mock(InstitutionalAccessContextMaterializationApplicationService.class);
    private final InstitutionalApiEdgeSecurityProfileApplicationService apiEdgeSecurityProfileApplicationService = mock(InstitutionalApiEdgeSecurityProfileApplicationService.class);
    private final InstitutionalRecertificationApplicationService recertificationApplicationService = mock(InstitutionalRecertificationApplicationService.class);
    private final InstitutionalRevocationApplicationService revocationApplicationService = mock(InstitutionalRevocationApplicationService.class);
    private final InstitutionalIntegrationSecurityPolicyApplicationService integrationSecurityPolicyApplicationService = mock(InstitutionalIntegrationSecurityPolicyApplicationService.class);
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport = mock(NationalCommunicationInstitutionalGovernanceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalAccessSecuritySurfaceService service = new NationalCommunicationInstitutionalAccessSecuritySurfaceService(
            accessContextMaterializationApplicationService, apiEdgeSecurityProfileApplicationService,
            recertificationApplicationService, revocationApplicationService, integrationSecurityPolicyApplicationService, governanceAssemblerSupport);

    @Test
    void contextoAcessoDelegaComAffiliationENominationId() {
        var snapshot = mock(InstitutionalAccessContextSnapshot.class);
        var response = mock(NationalCommunicationInstitutionalAccessContextResponse.class);
        when(accessContextMaterializationApplicationService.materializar("aff-1", "nom-1")).thenReturn(snapshot);
        when(governanceAssemblerSupport.toResponse(snapshot)).thenReturn(response);

        assertThat(service.contextoAcesso("aff-1", "nom-1")).isSameAs(response);
    }

    @Test
    void perfilSegurancaApiDelegaComAffiliationId() {
        var profile = mock(InstitutionalApiEdgeSecurityProfile.class);
        var response = mock(NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse.class);
        when(apiEdgeSecurityProfileApplicationService.avaliar("aff-2")).thenReturn(profile);
        when(governanceAssemblerSupport.toResponse(profile)).thenReturn(response);

        assertThat(service.perfilSegurancaApi("aff-2")).isSameAs(response);
    }

    @Test
    void recertificarPassaFundamentosDoRequest() {
        var request = new NationalCommunicationInstitutionalRecertificationRequest(List.of("fund-1"));
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRecertificationCycle.class);
        var response = mock(NationalCommunicationInstitutionalRecertificationResponse.class);
        when(recertificationApplicationService.recertificar("aff-3", List.of("fund-1"))).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.recertificar("aff-3", request)).isSameAs(response);
    }

    @Test
    void revogarAcessosPassaTodos5CamposDoRequest() {
        var request = new NationalCommunicationInstitutionalRevocationRequest(42L, "UNI-1", Boolean.TRUE, List.of("fund"));
        var result = mock(InstitutionalRevocationResult.class);
        var response = mock(NationalCommunicationInstitutionalRevocationResponse.class);
        when(revocationApplicationService.revogar("aff-4", 42L, "UNI-1", true, List.of("fund"))).thenReturn(result);
        when(governanceAssemblerSupport.toResponse(result)).thenReturn(response);

        assertThat(service.revogarAcessos("aff-4", request)).isSameAs(response);
    }

    @Test
    void revogarAcessosRequestNuloUsaNullsEFalseEListaVazia() {
        var result = mock(InstitutionalRevocationResult.class);
        var response = mock(NationalCommunicationInstitutionalRevocationResponse.class);
        when(revocationApplicationService.revogar("aff-5", null, null, false, List.of())).thenReturn(result);
        when(governanceAssemblerSupport.toResponse(result)).thenReturn(response);

        assertThat(service.revogarAcessos("aff-5", null)).isSameAs(response);
    }

    @Test
    void integracoesGovernancaDelegaComScopeEAffiliationId() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationSecurityPolicy.class);
        var response = mock(NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse.class);
        when(integrationSecurityPolicyApplicationService.listar("BR", "aff-6")).thenReturn(List.of(domain));
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.integracoesGovernanca("BR", "aff-6")).containsExactly(response);
    }
}
