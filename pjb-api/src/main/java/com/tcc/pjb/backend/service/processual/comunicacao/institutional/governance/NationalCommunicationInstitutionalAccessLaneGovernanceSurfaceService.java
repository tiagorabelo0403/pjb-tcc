package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAffiliationOnboardingPlanApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAuthenticationPolicyClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalProvisioningApplicationService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessProfileCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalAuthenticationPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOnboardingPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOrganizationBlueprintResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de NationalCommunicationInstitutionalGovernanceSurfaceFacadeService:
 * catálogo de acessos e blueprints organizacionais + plano de onboarding, política de
 * autenticação e provisionamento operacional -- toda a superfície de configuração de
 * lane de acesso institucional. facadeSupport compartilhado (bean singleton) só para
 * `blueprints(scope)` que precisa parsear a enum de escopo organizacional.
 */
@Service
public class NationalCommunicationInstitutionalAccessLaneGovernanceSurfaceService {

    private final InstitutionalAccessProfileCatalogApplicationService catalogService;
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;
    private final InstitutionalAffiliationOnboardingPlanApplicationService onboardingPlanApplicationService;
    private final InstitutionalAuthenticationPolicyClosureApplicationService authenticationPolicyClosureApplicationService;
    private final InstitutionalOperationalProvisioningApplicationService operationalProvisioningApplicationService;
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport;
    private final NationalCommunicationInstitutionalFacadeSupport facadeSupport;

    public NationalCommunicationInstitutionalAccessLaneGovernanceSurfaceService(
            InstitutionalAccessProfileCatalogApplicationService catalogService,
            InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService,
            InstitutionalAffiliationOnboardingPlanApplicationService onboardingPlanApplicationService,
            InstitutionalAuthenticationPolicyClosureApplicationService authenticationPolicyClosureApplicationService,
            InstitutionalOperationalProvisioningApplicationService operationalProvisioningApplicationService,
            NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport,
            NationalCommunicationInstitutionalFacadeSupport facadeSupport) {
        this.catalogService = catalogService;
        this.blueprintCatalogApplicationService = blueprintCatalogApplicationService;
        this.onboardingPlanApplicationService = onboardingPlanApplicationService;
        this.authenticationPolicyClosureApplicationService = authenticationPolicyClosureApplicationService;
        this.operationalProvisioningApplicationService = operationalProvisioningApplicationService;
        this.governanceAssemblerSupport = governanceAssemblerSupport;
        this.facadeSupport = facadeSupport;
    }

    public List<NationalCommunicationInstitutionalAccessProfileCatalogResponse> catalogoAcessos() {
        return catalogService.listarPerfis().stream().map(governanceAssemblerSupport::toResponse).toList();
    }

    public List<NationalCommunicationInstitutionalOrganizationBlueprintResponse> blueprints(String scope) {
        List<InstitutionalOrganizationBlueprint> items = scope == null || scope.isBlank()
                ? blueprintCatalogApplicationService.listar()
                : blueprintCatalogApplicationService.findByScope(facadeSupport.parseOrganizationScope(scope)).stream().toList();
        return items.stream().map(governanceAssemblerSupport::toResponse).toList();
    }

    public NationalCommunicationInstitutionalOnboardingPlanResponse planoOnboarding(String affiliationId) {
        return governanceAssemblerSupport.toResponse(onboardingPlanApplicationService.consolidar(affiliationId));
    }

    public NationalCommunicationInstitutionalAuthenticationPolicyResponse politicaAutenticacao(String affiliationId) {
        return governanceAssemblerSupport.toResponse(authenticationPolicyClosureApplicationService.consolidar(affiliationId));
    }

    public NationalCommunicationInstitutionalOperationalProvisioningResponse provisionamentoOperacional(String affiliationId) {
        return governanceAssemblerSupport.toResponse(operationalProvisioningApplicationService.consolidar(affiliationId));
    }

    public NationalCommunicationInstitutionalOperationalProvisioningResponse provisionarOperacional(String affiliationId,
                                                                                                    NationalCommunicationInstitutionalOperationalProvisioningRequest request) {
        return governanceAssemblerSupport.toResponse(operationalProvisioningApplicationService.provisionar(
                affiliationId,
                request != null && Boolean.TRUE.equals(request.persistExpandedBoxes()),
                request == null ? List.of() : request.fundamentos()));
    }
}
