package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAccessContextMaterializationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAffiliationOnboardingPlanApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalAuthenticationPolicyClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalManagedCredentialApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalRootAdministratorApprovalApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalStrongSignaturePolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalWorkloadIdentityPlanApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAccessContextSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalManagedCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalRootAdministratorApproval;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStrongSignaturePolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalWorkloadIdentityPlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalPublicRecognitionGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalSecureEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationApprovalTrailApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationValidationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalApiEdgeSecurityProfileApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalIntegrationCredentialApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalIntegrationSecurityPolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialIdentifierDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceAttestationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceConnectorCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceConnectorProbeApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRecertificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRemoteCertificateAuthorizationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRevocationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSensitiveActAuthorizationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSessionRiskApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalTrustGovernanceOrchestrationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationApprovalTrail;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalApiEdgeSecurityProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCallTrail;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationSecurityPolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalJudiciaryPopulationSizing;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierCheck;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestationItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceEvidence;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRecertificationCycle;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRemoteCertificateAuthorization;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRevocationResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSensitiveActAuthorization;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalCoverageDelegationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalProvisioningApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalUnitGovernanceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalProvisioningSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessLaneBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessProfileCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationHomologateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationValidationFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationValidationReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalSecureEntrySummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalApprovalTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalAuthenticationPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalLotationUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.InstitutionalRootAdminApprovalDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalRootAdministratorApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStrongSignaturePolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustGovernanceDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustGovernanceProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalUnitGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalWorkloadIdentityPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSimpleFundamentosRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIdentityBaseProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCallTrailCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCallTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierCheckResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceConnectorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceEvidenceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceRevalidationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.InstitutionalRemoteCertificateAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRevocationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRevocationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSessionRiskAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSessionRiskFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalManagedUnitUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOnboardingPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOrganizationBlueprintResponse;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NationalCommunicationInstitutionalGovernanceSurfaceFacadeService {

    private static final String OPERATIONAL_PROFILE_PROJECTION_COVERAGE_SENTINEL = "operationalProfileProjectionApplicationService.materializar(item.affiliationId(), item.nominationId())";

    private final InstitutionalAffiliationApplicationService affiliationService;
    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService;
    private final InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService;
    private final InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService;
    private final InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService;
    private final InstitutionalOfficialSourceConnectorCatalogApplicationService officialSourceConnectorCatalogApplicationService;
    private final InstitutionalOfficialSourceConnectorProbeApplicationService officialSourceConnectorProbeApplicationService;
    private final InstitutionalAccessProfileCatalogApplicationService catalogService;
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;
    private final InstitutionalAccessContextMaterializationApplicationService accessContextMaterializationApplicationService;
    private final InstitutionalAffiliationOnboardingPlanApplicationService onboardingPlanApplicationService;
    private final InstitutionalAuthenticationPolicyClosureApplicationService authenticationPolicyClosureApplicationService;
    private final InstitutionalOperationalProvisioningApplicationService operationalProvisioningApplicationService;
    private final InstitutionalManagedCredentialApplicationService managedCredentialApplicationService;
    private final InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService;
    private final InstitutionalStrongSignaturePolicyApplicationService strongSignaturePolicyApplicationService;
    private final InstitutionalUnitGovernanceApplicationService unitGovernanceApplicationService;
    private final InstitutionalWorkloadIdentityPlanApplicationService workloadIdentityPlanApplicationService;
    private final InstitutionalCoverageDelegationApplicationService coverageDelegationApplicationService;
    private final InstitutionalApiEdgeSecurityProfileApplicationService apiEdgeSecurityProfileApplicationService;
    private final InstitutionalRecertificationApplicationService recertificationApplicationService;
    private final InstitutionalRevocationApplicationService revocationApplicationService;
    private final InstitutionalIntegrationSecurityPolicyApplicationService integrationSecurityPolicyApplicationService;
    private final InstitutionalAffiliationValidationApplicationService validationApplicationService;
    private final InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService;
    private final InstitutionalRemoteCertificateAuthorizationApplicationService remoteCertificateAuthorizationApplicationService;
    private final InstitutionalSessionRiskApplicationService sessionRiskApplicationService;
    private final InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationApplicationService;
    private final InstitutionalIntegrationCredentialApplicationService integrationCredentialApplicationService;
    private final InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService;
    private final NationalCommunicationInstitutionalFacadeSupport facadeSupport;
    private final NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService;
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport;

    public NationalCommunicationInstitutionalGovernanceSurfaceFacadeService(
            InstitutionalAffiliationApplicationService affiliationService,
            InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService,
            InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService,
            InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService,
            InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService,
            InstitutionalOfficialSourceConnectorCatalogApplicationService officialSourceConnectorCatalogApplicationService,
            InstitutionalOfficialSourceConnectorProbeApplicationService officialSourceConnectorProbeApplicationService,
            InstitutionalAccessProfileCatalogApplicationService catalogService,
            InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService,
            InstitutionalAccessContextMaterializationApplicationService accessContextMaterializationApplicationService,
            InstitutionalAffiliationOnboardingPlanApplicationService onboardingPlanApplicationService,
            InstitutionalAuthenticationPolicyClosureApplicationService authenticationPolicyClosureApplicationService,
            InstitutionalOperationalProvisioningApplicationService operationalProvisioningApplicationService,
            InstitutionalManagedCredentialApplicationService managedCredentialApplicationService,
            InstitutionalRootAdministratorApprovalApplicationService rootAdministratorApprovalApplicationService,
            InstitutionalStrongSignaturePolicyApplicationService strongSignaturePolicyApplicationService,
            InstitutionalUnitGovernanceApplicationService unitGovernanceApplicationService,
            InstitutionalWorkloadIdentityPlanApplicationService workloadIdentityPlanApplicationService,
            InstitutionalCoverageDelegationApplicationService coverageDelegationApplicationService,
            InstitutionalApiEdgeSecurityProfileApplicationService apiEdgeSecurityProfileApplicationService,
            InstitutionalRecertificationApplicationService recertificationApplicationService,
            InstitutionalRevocationApplicationService revocationApplicationService,
            InstitutionalIntegrationSecurityPolicyApplicationService integrationSecurityPolicyApplicationService,
            InstitutionalAffiliationValidationApplicationService validationApplicationService,
            InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService,
            InstitutionalRemoteCertificateAuthorizationApplicationService remoteCertificateAuthorizationApplicationService,
            InstitutionalSessionRiskApplicationService sessionRiskApplicationService,
            InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationApplicationService,
            InstitutionalIntegrationCredentialApplicationService integrationCredentialApplicationService,
            InstitutionalTrustGovernanceOrchestrationApplicationService trustGovernanceOrchestrationApplicationService,
            NationalCommunicationInstitutionalFacadeSupport facadeSupport,
            NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService,
            NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport) {
        this.affiliationService = affiliationService;
        this.publicRecognitionGateApplicationService = publicRecognitionGateApplicationService;
        this.officialSourceDossierApplicationService = officialSourceDossierApplicationService;
        this.officialIdentifierDossierApplicationService = officialIdentifierDossierApplicationService;
        this.officialSourceAttestationApplicationService = officialSourceAttestationApplicationService;
        this.officialSourceConnectorCatalogApplicationService = officialSourceConnectorCatalogApplicationService;
        this.officialSourceConnectorProbeApplicationService = officialSourceConnectorProbeApplicationService;
        this.catalogService = catalogService;
        this.blueprintCatalogApplicationService = blueprintCatalogApplicationService;
        this.accessContextMaterializationApplicationService = accessContextMaterializationApplicationService;
        this.onboardingPlanApplicationService = onboardingPlanApplicationService;
        this.authenticationPolicyClosureApplicationService = authenticationPolicyClosureApplicationService;
        this.operationalProvisioningApplicationService = operationalProvisioningApplicationService;
        this.managedCredentialApplicationService = managedCredentialApplicationService;
        this.rootAdministratorApprovalApplicationService = rootAdministratorApprovalApplicationService;
        this.strongSignaturePolicyApplicationService = strongSignaturePolicyApplicationService;
        this.unitGovernanceApplicationService = unitGovernanceApplicationService;
        this.workloadIdentityPlanApplicationService = workloadIdentityPlanApplicationService;
        this.coverageDelegationApplicationService = coverageDelegationApplicationService;
        this.apiEdgeSecurityProfileApplicationService = apiEdgeSecurityProfileApplicationService;
        this.recertificationApplicationService = recertificationApplicationService;
        this.revocationApplicationService = revocationApplicationService;
        this.integrationSecurityPolicyApplicationService = integrationSecurityPolicyApplicationService;
        this.validationApplicationService = validationApplicationService;
        this.approvalTrailApplicationService = approvalTrailApplicationService;
        this.remoteCertificateAuthorizationApplicationService = remoteCertificateAuthorizationApplicationService;
        this.sessionRiskApplicationService = sessionRiskApplicationService;
        this.sensitiveActAuthorizationApplicationService = sensitiveActAuthorizationApplicationService;
        this.integrationCredentialApplicationService = integrationCredentialApplicationService;
        this.trustGovernanceOrchestrationApplicationService = trustGovernanceOrchestrationApplicationService;
        this.facadeSupport = facadeSupport;
        this.stateBundleFacadeService = stateBundleFacadeService;
        this.governanceAssemblerSupport = governanceAssemblerSupport;
    }

    public NationalCommunicationInstitutionalAffiliationResponse solicitarAfiliacao(NationalCommunicationInstitutionalAffiliationCreateRequest request) {
        return toAffiliation(affiliationService.solicitarAfiliacao(
                facadeSupport.parseDestinatarioKind(request.destinatarioInstitucionalKind()),
                facadeSupport.parseOrganizationScope(request.organizationScope()),
                request.orgaoSigla(),
                request.orgaoNome(),
                request.unidadeCodigo(),
                request.unidadeNome(),
                request.uf(),
                request.comarca(),
                request.cnpj(),
                request.esferaAdministrativa(),
                request.ramosMateriais(),
                request.abrangenciasTerritoriais(),
                request.dominioInstitucional(),
                request.autoridadeAderenteCargo(),
                request.emailContatoSeguranca(),
                facadeSupport.parseNominationRole(request.representativeRole()),
                facadeSupport.parseTrustLevel(request.trustFloor()),
                request.requerDuplaAprovacaoAdministrador(),
                request.requerCertificadoICP(),
                request.restringeCertificadoRedeInstitucional(),
                request.permiteUsoRemotoComAutorizacao(),
                request.canaisHabilitados(),
                request.politicaCiencia(),
                request.sla(),
                request.regrasFallback(),
                request.conveniosIntegracoes(),
                request.fundamentos()));
    }

    public NationalCommunicationInstitutionalAffiliationResponse homologarAfiliacao(String affiliationId, NationalCommunicationInstitutionalAffiliationHomologateRequest request) {
        return toAffiliation(affiliationService.homologarAfiliacao(affiliationId, request.homologar(), request.fundamentos()));
    }

    public List<NationalCommunicationInstitutionalAffiliationResponse> afiliacoes() {
        return affiliationService.listarAfiliacoes().stream().map(this::toAffiliation).toList();
    }

    public AdminInstitutionalPublicRecognitionResponse reconhecimentoPublicoAfiliacao(String affiliationId) {
        return publicRecognitionGateApplicationService.avaliarAfiliacao(affiliationId);
    }

    public NationalCommunicationInstitutionalOfficialSourceDossierResponse dossieFontesOficiaisAfiliacao(String affiliationId) {
        return toOfficialSourceDossierResponse(officialSourceDossierApplicationService.gerarAfiliacao(affiliationId));
    }

    public NationalCommunicationInstitutionalOfficialIdentifierDossierResponse identificadoresOficiaisAfiliacao(String affiliationId) {
        return toOfficialIdentifierDossierResponse(officialIdentifierDossierApplicationService.gerarAfiliacao(affiliationId));
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse atestacaoFontesOficiaisAfiliacao(String affiliationId) {
        return toOfficialSourceAttestationResponse(officialSourceAttestationApplicationService.consultarAfiliacao(affiliationId));
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse revalidarFontesOficiaisAfiliacao(String affiliationId,
                                                                                                                  NationalCommunicationInstitutionalOfficialSourceRevalidationRequest request) {
        return toOfficialSourceAttestationResponse(officialSourceAttestationApplicationService.revalidarAfiliacao(
                affiliationId,
                request == null ? List.of() : request.fundamentos()));
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse catalogoConectoresFontesOficiais() {
        return officialSourceConnectorCatalogApplicationService.listar();
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorCatalogResponse sondarConectoresFontesOficiais() {
        return officialSourceConnectorProbeApplicationService.sondarTodos();
    }

    public NationalCommunicationInstitutionalOfficialSourceConnectorResponse sondarConectorFonteOficial(String sourceCode) {
        return officialSourceConnectorProbeApplicationService.sondar(sourceCode);
    }

    public NationalCommunicationInstitutionalNominationResponse nomear(NationalCommunicationInstitutionalNominationCreateRequest request) {
        return toNomination(affiliationService.nomearPessoa(
                request.affiliationId(),
                request.nominatedUserId(),
                request.nominatedUserName(),
                request.tipoUsuario() == null ? null : TipoUsuario.fromPerfil(request.tipoUsuario()),
                facadeSupport.parseAccessLaneKind(request.accessLaneKind()),
                facadeSupport.parseNominationRole(request.nominationRole()),
                facadeSupport.parseFuncaoOperacional(request.funcaoOperacional()),
                facadeSupport.parseProcessProfile(request.processProfile()),
                request.unidadeCodigo(),
                request.caixaCodigo(),
                facadeSupport.parseCapacidades(request.capacidades()),
                facadeSupport.parseTrustLevel(request.trustFloor()),
                facadeSupport.parseLandingPanel(request.panelPreferencial()),
                request.ativaDe(),
                request.ativaAte(),
                request.requerStepUpMfa(),
                request.requerCertificadoICP(),
                request.requerRedeInstitucional(),
                request.permiteUsoRemotoAutorizado()));
    }

    public List<NationalCommunicationInstitutionalNominationResponse> nomeacoes(Long userId) {
        return affiliationService.listarNomeacoes(userId).stream().map(this::toNomination).toList();
    }

    public NationalCommunicationInstitutionalSecureEntrySummaryResponse entradaSegura() {
        InstitutionalSecureEntrySummary summary = affiliationService.avaliarEntradaSeguraAtual();
        return governanceAssemblerSupport.toResponse(summary);
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
                                                                                                   com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
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

    public NationalCommunicationInstitutionalUnitGovernanceResponse governancaUnidades(String affiliationId) {
        return governanceAssemblerSupport.toResponse(unitGovernanceApplicationService.consolidar(affiliationId));
    }

    public NationalCommunicationInstitutionalUnitGovernanceResponse registrarUnidade(String affiliationId,
                                                                                      NationalCommunicationInstitutionalManagedUnitUpsertRequest request) {
        return governanceAssemblerSupport.toResponse(unitGovernanceApplicationService.registrarUnidade(affiliationId, request));
    }

    public NationalCommunicationInstitutionalUnitGovernanceResponse registrarLotacao(String affiliationId,
                                                                                      NationalCommunicationInstitutionalLotationUpsertRequest request) {
        return governanceAssemblerSupport.toResponse(unitGovernanceApplicationService.registrarLotacao(affiliationId, request));
    }

    public NationalCommunicationInstitutionalWorkloadIdentityPlanResponse identidadeWorkload(String affiliationId) {
        InstitutionalWorkloadIdentityPlan plan = workloadIdentityPlanApplicationService.avaliar(affiliationId);
        return governanceAssemblerSupport.toResponse(plan);
    }

    public NationalCommunicationInstitutionalCoverageDelegationResponse delegacoesCobertura(String affiliationId) {
        return governanceAssemblerSupport.toResponse(coverageDelegationApplicationService.consolidar(affiliationId));
    }

    public NationalCommunicationInstitutionalCoverageDelegationResponse registrarDelegacaoCobertura(String affiliationId,
                                                                                                    NationalCommunicationInstitutionalCoverageDelegationUpsertRequest request) {
        return governanceAssemblerSupport.toResponse(coverageDelegationApplicationService.registrar(affiliationId, request));
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

    public NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse dimensionamentoUsuariosInternos() {
        return governanceAssemblerSupport.toResponse(trustGovernanceOrchestrationApplicationService.dimensionarUsuariosInternos());
    }

    public NationalCommunicationInstitutionalTrustGovernanceProfileResponse governancaConfianca(String affiliationId, String nominationId) {
        return governanceAssemblerSupport.toResponse(stateBundleFacadeService.carregar(affiliationId, nominationId).trustGovernanceProfile());
    }

    public NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse planoDadosHorizontal(String affiliationId, String nominationId) {
        return governanceAssemblerSupport.toResponse(stateBundleFacadeService.carregar(affiliationId, nominationId).horizontalDataPlanePlan());
    }

    public NationalCommunicationInstitutionalOperationalProfileResponse perfilOperacional(String affiliationId, String nominationId) {
        return governanceAssemblerSupport.toResponse(stateBundleFacadeService.materializarPerfil(affiliationId, nominationId));
    }

    public NationalCommunicationInstitutionalTrustGovernanceProfileResponse decidirGovernancaConfianca(String affiliationId,
                                                                                                        String nominationId,
                                                                                                        NationalCommunicationInstitutionalTrustGovernanceDecisionRequest request) {
        com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustApprovalKind approvalKind =
                com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustApprovalKind.fromTexto(request == null ? null : request.approvalKind());
        if (approvalKind == null) {
            throw new IllegalArgumentException("Tipo de aprovação institucional não reconhecido.");
        }
        return governanceAssemblerSupport.toResponse(trustGovernanceOrchestrationApplicationService.decidir(
                affiliationId,
                nominationId,
                approvalKind,
                request.approved(),
                request.fundamentos()));
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

    private NationalCommunicationInstitutionalOfficialIdentifierDossierResponse toOfficialIdentifierDossierResponse(InstitutionalOfficialIdentifierDossier dossier) {
        return new NationalCommunicationInstitutionalOfficialIdentifierDossierResponse(
                dossier.subjectType(),
                dossier.subjectId(),
                dossier.affiliationId(),
                dossier.requestId(),
                dossier.organizationScope(),
                dossier.orgaoSigla(),
                dossier.unidadeCodigo(),
                dossier.overallStatus(),
                dossier.materialEvidenceReady(),
                dossier.generatedAt(),
                dossier.blockingIssues(),
                dossier.checks().stream().map(this::toOfficialIdentifierCheckResponse).toList(),
                dossier.fundamentos(),
                dossier.integrityHash()
        );
    }

    private NationalCommunicationInstitutionalOfficialIdentifierCheckResponse toOfficialIdentifierCheckResponse(InstitutionalOfficialIdentifierCheck check) {
        return new NationalCommunicationInstitutionalOfficialIdentifierCheckResponse(
                check.identifierCode(),
                check.identifierLabel(),
                check.sourceCode(),
                check.value(),
                check.normalizedValue(),
                check.status(),
                check.applicable(),
                check.requiredForRecognition(),
                check.readyForRemoteLookup(),
                check.connectorStatus(),
                check.officialLookupUrl(),
                check.evidenceSignals(),
                check.pendingIssues(),
                check.fundamentos(),
                check.integrityHash()
        );
    }

    private NationalCommunicationInstitutionalOfficialSourceAttestationResponse toOfficialSourceAttestationResponse(InstitutionalOfficialSourceAttestation attestation) {
        return new NationalCommunicationInstitutionalOfficialSourceAttestationResponse(
                attestation.subjectType(),
                attestation.subjectId(),
                attestation.affiliationId(),
                attestation.requestId(),
                attestation.organizationScope(),
                attestation.orgaoSigla(),
                attestation.unidadeCodigo(),
                attestation.publicRecognitionStatus(),
                attestation.attestationStatus(),
                attestation.sovereignRecognitionReady(),
                attestation.dueNow(),
                attestation.automaticRefreshEligible(),
                attestation.lastAttestedAt(),
                attestation.nextRefreshAt(),
                attestation.blockingIssues(),
                attestation.sources().stream().map(this::toOfficialSourceAttestationItemResponse).toList(),
                attestation.fundamentos(),
                attestation.integrityHash()
        );
    }

    private NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse toOfficialSourceAttestationItemResponse(InstitutionalOfficialSourceAttestationItem item) {
        return new NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse(
                item.sourceCode(),
                item.sourceLabel(),
                item.authority(),
                item.authorityScope(),
                item.accessMode(),
                item.refreshMode(),
                item.directGovernmentSource(),
                item.autoRefreshSupported(),
                item.applicable(),
                item.satisfied(),
                item.mandatoryForAutomaticActivation(),
                item.stale(),
                item.refreshRecommended(),
                item.confidenceScore(),
                item.confidenceBand(),
                item.lastVerifiedAt(),
                item.nextRefreshAt(),
                item.integrityHash(),
                item.connectorStatus(),
                item.connectorEnabled(),
                item.connectorLiveVerificationSupported(),
                item.connectorReferenceUrl(),
                item.connectorCheckedAt(),
                item.connectorNextCheckAt(),
                item.connectorSignals(),
                item.connectorBlockers(),
                item.evidenceSignals(),
                item.pendingIssues(),
                item.safeNextSteps(),
                item.fundamentos()
        );
    }

    private NationalCommunicationInstitutionalOfficialSourceDossierResponse toOfficialSourceDossierResponse(InstitutionalOfficialSourceDossier dossier) {
        return new NationalCommunicationInstitutionalOfficialSourceDossierResponse(
                dossier.subjectType(),
                dossier.subjectId(),
                dossier.affiliationId(),
                dossier.requestId(),
                dossier.organizationScope(),
                dossier.orgaoSigla(),
                dossier.unidadeCodigo(),
                dossier.publicRecognitionStatus(),
                dossier.sovereignRecognitionReady(),
                dossier.dueNow(),
                dossier.nextMandatoryReviewAt(),
                dossier.blockingIssues(),
                dossier.sources().stream().map(this::toOfficialSourceEvidenceResponse).toList(),
                dossier.fundamentos(),
                dossier.generatedAt()
        );
    }

    private NationalCommunicationInstitutionalOfficialSourceEvidenceResponse toOfficialSourceEvidenceResponse(InstitutionalOfficialSourceEvidence evidence) {
        return new NationalCommunicationInstitutionalOfficialSourceEvidenceResponse(
                evidence.sourceCode(),
                evidence.sourceLabel(),
                evidence.sourceGroup(),
                evidence.applicable(),
                evidence.satisfied(),
                evidence.mandatoryForAutomaticActivation(),
                evidence.stale(),
                evidence.lastEvidenceAt(),
                evidence.nextReviewAt(),
                evidence.evidenceSignals(),
                evidence.pendingIssues(),
                evidence.fundamentos()
        );
    }

    private NationalCommunicationInstitutionalAffiliationResponse toAffiliation(InstitutionalAffiliation item) {
        return governanceAssemblerSupport.toResponse(item);
    }

    private NationalCommunicationInstitutionalNominationResponse toNomination(InstitutionalNomination item) {
        return governanceAssemblerSupport.toResponse(item);
    }
}