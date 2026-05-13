package com.tcc.pjb.backend.architecture;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationAcknowledgeRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationCanonicalActResolveRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationCanonicalActResolveResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationDispatchRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationDispatchResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.flow.NationalCommunicationFallbackRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalFourLevelAccessResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalIdentityGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalMembershipResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalCanonicalCatalogEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalProvisionedDirectoryEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalSecureEntrySummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalSemanticTimelineEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalAnalyticsDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalExecutiveDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalObservabilityDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTriageSuggestionDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalUnitQueueResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessSeparatorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessVisualLaneResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalHardeningFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalHardeningReportResponse;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class PjbProcessualCommunicationPackageOrganizationArchTest {

    @ArchTest
    static final ArchRule generic_communication_flow_dtos_must_live_in_flow_or_routing =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationAcknowledgeRequest")
                    .or().haveSimpleName("NationalCommunicationCanonicalActResolveRequest")
                    .or().haveSimpleName("NationalCommunicationCanonicalActResolveResponse")
                    .or().haveSimpleName("NationalCommunicationDashboardResponse")
                    .or().haveSimpleName("NationalCommunicationDispatchRequest")
                    .or().haveSimpleName("NationalCommunicationDispatchResponse")
                    .or().haveSimpleName("NationalCommunicationFallbackRequest")
                    .or().haveSimpleNameStartingWith("NationalCommunicationRouting")
                    .or().haveSimpleNameStartingWith("NationalCommunicationProcessualRecipient")
                    .should().resideInAnyPackage(
                            "..model.dto.processual.comunicacao.flow..",
                            "..model.dto.processual.comunicacao.routing..");

    @ArchTest
    static final ArchRule institutional_access_dtos_must_live_in_access_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("NationalCommunicationInstitutionalAccess")
                    .or().haveSimpleName("NationalCommunicationInstitutionalFourLevelAccessResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalIdentityGuardResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalMembershipResponse")
                    .should().resideInAPackage("..model.dto.processual.comunicacao.institutional.access..");

    @ArchTest
    static final ArchRule institutional_affiliation_dtos_must_live_in_affiliation_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("NationalCommunicationInstitutionalAffiliation")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalDelegatedAffiliation")
                    .or().haveSimpleName("NationalCommunicationInstitutionalDelegateRequest")
                    .or().haveSimpleName("NationalCommunicationInstitutionalDelegationResponse")
                    .should().resideInAPackage("..model.dto.processual.comunicacao.institutional.affiliation..");

    @ArchTest
    static final ArchRule institutional_entry_dtos_must_live_in_entry_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("NationalCommunicationInstitutionalEntry")
                    .or().haveSimpleName("NationalCommunicationInstitutionalCanonicalCatalogEntryResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalProvisionedDirectoryEntryResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalSecureEntrySummaryResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalSemanticTimelineEntryResponse")
                    .should().resideInAPackage("..model.dto.processual.comunicacao.institutional.entry..");

    @ArchTest
    static final ArchRule institutional_panel_dtos_must_live_in_panel_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("NationalCommunicationInstitutionalPanel")
                    .or().haveSimpleName("NationalCommunicationInstitutionalAnalyticsDashboardResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalExecutiveDashboardResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalObservabilityDashboardResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalTriageSuggestionDashboardResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalUnitQueueResponse")
                    .should().resideInAPackage("..model.dto.processual.comunicacao.institutional.panel..");

    @ArchTest
    static final ArchRule institutional_procedural_dtos_must_live_in_procedural_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("NationalCommunicationInstitutionalProcedural")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalProcessDiagnostic")
                    .or().haveSimpleName("NationalCommunicationInstitutionalProcessActionResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalProcessSeparatorResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalProcessVisualLaneResponse")
                    .should().resideInAPackage("..model.dto.processual.comunicacao.institutional.procedural..");

    @ArchTest
    static final ArchRule institutional_workspace_dtos_must_live_in_workspace_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..model.dto..").and().haveSimpleNameStartingWith("NationalCommunicationInstitutionalProcessWorkspace")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalStructuralDiagnostic")
                    .should().resideInAPackage("..model.dto.processual.comunicacao.institutional.workspace..");

    @ArchTest
    static final ArchRule institutional_security_dtos_must_live_in_security_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleNameStartingWith("NationalCommunicationInstitutionalIntegration")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalOfficialSource")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalOfficialIdentifier")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalManagedCredential")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalRemoteCertificateAuthorization")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalSensitiveActAuthorization")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalSessionRisk")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalRecertification")
                    .or().haveSimpleNameStartingWith("NationalCommunicationInstitutionalRevocation")
                    .or().haveSimpleName("NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalHardeningFindingResponse")
                    .or().haveSimpleName("NationalCommunicationInstitutionalHardeningReportResponse")
                    .should().resideInAPackage("..model.dto.processual.comunicacao.institutional.security..");


    @ArchTest
    static final ArchRule communication_flow_services_must_live_in_flow_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationFlowFacade")
                    .or().haveSimpleName("NationalCommunicationFlowRoutes")
                    .or().haveSimpleName("NationalCommunicationFlowService")
                    .should().resideInAPackage("..service.processual.comunicacao.flow..");

    @ArchTest
    static final ArchRule institutional_access_services_must_live_in_access_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("InstitutionalRequestAccessContextFacadeService")
                    .should().resideInAPackage("..service.processual.comunicacao.institutional.access..");

    @ArchTest
    static final ArchRule institutional_state_services_must_live_in_state_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalStateBundle")
                    .or().haveSimpleName("NationalCommunicationInstitutionalStateBundleFacadeService")
                    .should().resideInAPackage("..service.processual.comunicacao.institutional.state..");

    @ArchTest
    static final ArchRule institutional_panel_services_must_live_in_panel_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalPanelAssemblerSupport")
                    .or().haveSimpleName("NationalCommunicationInstitutionalPanelFacadeService")
                    .should().resideInAPackage("..service.processual.comunicacao.institutional.panel..");

    @ArchTest
    static final ArchRule institutional_governance_services_must_live_in_governance_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalGovernanceAssemblerSupport")
                    .or().haveSimpleName("NationalCommunicationInstitutionalGovernanceSurfaceFacadeService")
                    .should().resideInAPackage("..service.processual.comunicacao.institutional.governance..");

    @ArchTest
    static final ArchRule institutional_surface_services_must_live_in_surface_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalSurfaceAssemblerSupport")
                    .or().haveSimpleName("NationalCommunicationInstitutionalSurfaceFacadeService")
                    .should().resideInAPackage("..service.processual.comunicacao.institutional.surface..");

    @ArchTest
    static final ArchRule institutional_support_services_must_live_in_support_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalFacadeSupport")
                    .should().resideInAPackage("..service.processual.comunicacao.institutional.support..");

    @ArchTest
    static final ArchRule institutional_operations_facade_must_live_in_operations_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalOperationsFacade")
                    .should().resideInAPackage("..service.processual.comunicacao.institutional.operations..");


    @ArchTest
    static final ArchRule communication_flow_controllers_must_live_in_flow_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationFlowController")
                    .should().resideInAPackage("..controller.processual.comunicacao.flow..");

    @ArchTest
    static final ArchRule institutional_access_controllers_must_live_in_access_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalAccessClosureController")
                    .or().haveSimpleName("NationalCommunicationInstitutionalAccessOrchestrationController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.access..");

    @ArchTest
    static final ArchRule institutional_affiliation_controllers_must_live_in_affiliation_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalAffiliationController")
                    .or().haveSimpleName("NationalCommunicationInstitutionalDelegatedOnboardingController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.affiliation..");

    @ArchTest
    static final ArchRule institutional_entry_controllers_must_live_in_entry_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalEntryController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.entry..");

    @ArchTest
    static final ArchRule institutional_governance_controllers_must_live_in_governance_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalAdvancedController")
                    .or().haveSimpleName("NationalCommunicationInstitutionalClosureController")
                    .or().haveSimpleName("NationalCommunicationInstitutionalDelegatedClosureController")
                    .or().haveSimpleName("NationalCommunicationInstitutionalGovernanceHardeningController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.governance..");

    @ArchTest
    static final ArchRule institutional_operations_controllers_must_live_in_operations_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalLifecycleController")
                    .or().haveSimpleName("NationalCommunicationInstitutionalOperationsController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.operations..");

    @ArchTest
    static final ArchRule institutional_panel_controllers_must_live_in_panel_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalFinalController")
                    .or().haveSimpleName("NationalCommunicationInstitutionalPanelController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.panel..");

    @ArchTest
    static final ArchRule institutional_procedural_controllers_must_live_in_procedural_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalProceduralCoherenceController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.procedural..");

    @ArchTest
    static final ArchRule institutional_security_controllers_must_live_in_security_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalSecurityGovernanceController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.security..");

    @ArchTest
    static final ArchRule institutional_topology_controllers_must_live_in_topology_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalTopologyController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.topology..");

    @ArchTest
    static final ArchRule institutional_workspace_controllers_must_live_in_workspace_package =
            com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().haveSimpleName("NationalCommunicationInstitutionalProcessWorkspaceController")
                    .should().resideInAPackage("..controller.processual.comunicacao.institutional.workspace..");

}
