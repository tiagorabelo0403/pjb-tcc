package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAffiliationOnboardingPlanApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAuthenticationPolicyClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalProvisioningApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessProfileCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalAuthenticationPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOnboardingPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOrganizationBlueprintResponse;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalAccessLaneGovernanceSurfaceServiceTest {

    private final InstitutionalAccessProfileCatalogApplicationService catalogService = mock(InstitutionalAccessProfileCatalogApplicationService.class);
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService = mock(InstitutionalOrganizationBlueprintCatalogApplicationService.class);
    private final InstitutionalAffiliationOnboardingPlanApplicationService onboardingPlanApplicationService = mock(InstitutionalAffiliationOnboardingPlanApplicationService.class);
    private final InstitutionalAuthenticationPolicyClosureApplicationService authenticationPolicyClosureApplicationService = mock(InstitutionalAuthenticationPolicyClosureApplicationService.class);
    private final InstitutionalOperationalProvisioningApplicationService operationalProvisioningApplicationService = mock(InstitutionalOperationalProvisioningApplicationService.class);
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport = mock(NationalCommunicationInstitutionalGovernanceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalFacadeSupport facadeSupport = mock(NationalCommunicationInstitutionalFacadeSupport.class);
    private final NationalCommunicationInstitutionalAccessLaneGovernanceSurfaceService service = new NationalCommunicationInstitutionalAccessLaneGovernanceSurfaceService(
            catalogService, blueprintCatalogApplicationService, onboardingPlanApplicationService,
            authenticationPolicyClosureApplicationService, operationalProvisioningApplicationService,
            governanceAssemblerSupport, facadeSupport);

    @Test
    void blueprintsSemScopeChamaListarSemFiltro() {
        var domain = mock(InstitutionalOrganizationBlueprint.class);
        var response = mock(NationalCommunicationInstitutionalOrganizationBlueprintResponse.class);
        when(blueprintCatalogApplicationService.listar()).thenReturn(List.of(domain));
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.blueprints(null)).containsExactly(response);
        assertThat(service.blueprints("  ")).containsExactly(response);
    }

    @Test
    void blueprintsComScopeParseiaEChamaFindByScope() {
        var domain = mock(InstitutionalOrganizationBlueprint.class);
        var response = mock(NationalCommunicationInstitutionalOrganizationBlueprintResponse.class);
        when(facadeSupport.parseOrganizationScope("FORUM")).thenReturn(InstitutionalOrganizationScope.FORUM);
        when(blueprintCatalogApplicationService.findByScope(InstitutionalOrganizationScope.FORUM)).thenReturn(java.util.Optional.of(domain));
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.blueprints("FORUM")).containsExactly(response);
    }

    @Test
    void planoOnboardingDelegaComAffiliationId() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAffiliationOnboardingPlan.class);
        var response = mock(NationalCommunicationInstitutionalOnboardingPlanResponse.class);
        when(onboardingPlanApplicationService.consolidar("aff-1")).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.planoOnboarding("aff-1")).isSameAs(response);
    }

    @Test
    void politicaAutenticacaoDelegaComAffiliationId() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAuthenticationPolicyClosure.class);
        var response = mock(NationalCommunicationInstitutionalAuthenticationPolicyResponse.class);
        when(authenticationPolicyClosureApplicationService.consolidar("aff-2")).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.politicaAutenticacao("aff-2")).isSameAs(response);
    }

    @Test
    void provisionarOperacionalPassaFlagsEFundamentosDoRequest() {
        var request = new NationalCommunicationInstitutionalOperationalProvisioningRequest(Boolean.TRUE, List.of("fundamento-1"));
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalProvisioningSnapshot.class);
        var response = mock(NationalCommunicationInstitutionalOperationalProvisioningResponse.class);
        when(operationalProvisioningApplicationService.provisionar("aff-3", true, List.of("fundamento-1"))).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.provisionarOperacional("aff-3", request)).isSameAs(response);
    }

    @Test
    void provisionarOperacionalRequestNuloUsaFalseEListaVazia() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalProvisioningSnapshot.class);
        var response = mock(NationalCommunicationInstitutionalOperationalProvisioningResponse.class);
        when(operationalProvisioningApplicationService.provisionar("aff-4", false, List.of())).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.provisionarOperacional("aff-4", null)).isSameAs(response);
    }

    @Test
    void catalogoAcessosDelegaEMapeiaLista() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry.class);
        var response = mock(NationalCommunicationInstitutionalAccessProfileCatalogResponse.class);
        when(catalogService.listarPerfis()).thenReturn(List.of(domain));
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.catalogoAcessos()).containsExactly(response);
    }
}
