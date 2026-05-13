package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAccessContextSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAffiliationOnboardingPlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAuthenticationLanePolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalAuthenticationPolicyClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalManagedCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalOnboardingStep;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalRootAdministratorApproval;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStrongSignaturePolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalWorkloadIdentityBinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalWorkloadIdentityPlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalSecureEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalIntegrationCredentialApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationApprovalTrail;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalApiEdgeSecurityProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCallTrail;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCredential;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationSecurityPolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalJudiciaryPopulationSizing;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRecertificationCycle;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRemoteCertificateAuthorization;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRevocationResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSensitiveActAuthorization;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustGovernanceProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalLotationGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalManagedUnitEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalProvisioningSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalProvisionedDirectoryEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessLaneBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessProfileCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationValidationFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationValidationReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalProvisionedDirectoryEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalSecureEntrySummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalApprovalTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalAuthenticationLanePolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalAuthenticationPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalLotationGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalNominationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalRootAdministratorApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStrongSignaturePolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustGovernanceProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalUnitGovernanceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalWorkloadIdentityBindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalWorkloadIdentityPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProvisioningResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIdentityBaseProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCallTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalManagedCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRevocationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSessionRiskAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSessionRiskFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalManagedUnitResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOnboardingPlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOnboardingStepResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOrganizationBlueprintResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NationalCommunicationInstitutionalGovernanceAssemblerSupport {

    private final NationalCommunicationInstitutionalFacadeSupport facadeSupport;
    private final NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService;

    public NationalCommunicationInstitutionalGovernanceAssemblerSupport(
            NationalCommunicationInstitutionalFacadeSupport facadeSupport,
            NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService) {
        this.facadeSupport = Objects.requireNonNull(facadeSupport, "facadeSupport");
        this.stateBundleFacadeService = Objects.requireNonNull(stateBundleFacadeService, "stateBundleFacadeService");
    }

    public NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse toResponse(InstitutionalJudiciaryPopulationSizing item) {
        return facadeSupport.toResponse(item);
    }

    public NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse toResponse(InstitutionalHorizontalDataPlanePlan item) {
        return facadeSupport.toResponse(item);
    }

    public NationalCommunicationInstitutionalOperationalProfileResponse toResponse(InstitutionalOperationalProfileProjection item) {
        return facadeSupport.toResponse(item);
    }

    public NationalCommunicationInstitutionalTrustGovernanceProfileResponse toResponse(InstitutionalTrustGovernanceProfile item) {
        return facadeSupport.toResponse(item);
    }

    public NationalCommunicationInstitutionalAffiliationResponse toResponse(InstitutionalAffiliation item) {
        return facadeSupport.toAffiliation(item);
    }

    public NationalCommunicationInstitutionalNominationResponse toResponse(InstitutionalNomination item) {
        NationalCommunicationInstitutionalOperationalProfileResponse operationalProfile = facadeSupport.toResponse(
                stateBundleFacadeService.materializarPerfil(item.affiliationId(), item.nominationId()));
        return facadeSupport.toNomination(item, operationalProfile);
    }

    public NationalCommunicationInstitutionalIdentityBaseProfileResponse toResponse(InstitutionalIdentityBaseProfile item) {
        return facadeSupport.toIdentityBase(item);
    }

    public NationalCommunicationInstitutionalTrustAssessmentResponse toResponse(InstitutionalTrustAssessment item) {
        return facadeSupport.toAssessment(item);
    }

    public NationalCommunicationInstitutionalEntryContextResponse toResponse(InstitutionalEntryContext item) {
        return facadeSupport.toContext(item);
    }

    public NationalCommunicationInstitutionalSecureEntrySummaryResponse toResponse(InstitutionalSecureEntrySummary summary) {
        return new NationalCommunicationInstitutionalSecureEntrySummaryResponse(
                toResponse(summary.identityBaseProfile()),
                toResponse(summary.assessment()),
                summary.activeAffiliations().stream().map(this::toResponse).toList(),
                summary.activeNominations().stream().map(this::toResponse).toList(),
                summary.compatibleContexts().stream().map(this::toResponse).toList(),
                summary.generatedAt());
    }

    public NationalCommunicationInstitutionalAccessProfileCatalogResponse toResponse(InstitutionalAccessProfileCatalogEntry item) {
        return new NationalCommunicationInstitutionalAccessProfileCatalogResponse(
                item.codigo(),
                item.nomeExibicao(),
                item.entryMode().name(),
                item.nominationRole() == null ? null : item.nominationRole().name(),
                item.processProfile().name(),
                item.panel().name(),
                item.trustFloor().name(),
                item.capacidadesPadrao().stream().map(Enum::name).toList(),
                item.restricoes(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalOrganizationBlueprintResponse toResponse(InstitutionalOrganizationBlueprint item) {
        return new NationalCommunicationInstitutionalOrganizationBlueprintResponse(
                item.codigo(),
                item.scope().name(),
                item.nomeExibicao(),
                item.destinatarioKind().name(),
                item.organizacaoKind().name(),
                item.entryMode().name(),
                item.trustFloorPadrao().name(),
                item.requerCertificadoICP(),
                item.restringeCertificadoRedeInstitucional(),
                item.permiteUsoRemotoComAutorizacao(),
                item.requerDuplaAprovacaoAdministrador(),
                item.lanes().stream().map(this::toResponse).toList(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalAccessContextResponse toResponse(InstitutionalAccessContextSnapshot item) {
        return new NationalCommunicationInstitutionalAccessContextResponse(
                item.profileKey(),
                item.affiliationId(),
                item.nominationId(),
                item.panelCode(),
                item.processAreaCode(),
                item.primaryUnitCode(),
                item.primaryBoxCode(),
                item.coverageMode(),
                item.horizontalDataPlaneKey(),
                item.primaryWritePartitionKey(),
                item.readReplicaCode(),
                item.trustFloor(),
                item.readyForInstitutionalPanel(),
                item.fullyApproved(),
                item.readOnly(),
                item.requiresStepUp(),
                item.requiresQualifiedCertificate(),
                item.judicialFlowSensitive(),
                item.rlsScopeKey(),
                item.allowedUnitCodes(),
                item.allowedBoxCodes(),
                item.allowedLaneCodes(),
                item.activeCoverageDelegationIds(),
                item.restrictionTags(),
                item.sessionVariables(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalAccessLaneBlueprintResponse toResponse(InstitutionalAccessLaneBlueprint item) {
        return new NationalCommunicationInstitutionalAccessLaneBlueprintResponse(
                item.laneKind().name(),
                item.codigo(),
                item.nomeExibicao(),
                item.nominationRole().name(),
                item.funcaoOperacional().name(),
                item.processProfile().name(),
                item.panel().name(),
                item.trustFloor().name(),
                item.capacidadesPadrao().stream().map(Enum::name).toList(),
                item.requerStepUpMfa(),
                item.requerCertificadoICP(),
                item.requerRedeInstitucional(),
                item.permiteUsoRemotoAutorizado(),
                item.restricoes(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalAuthenticationLanePolicyResponse toResponse(InstitutionalAuthenticationLanePolicy item) {
        return new NationalCommunicationInstitutionalAuthenticationLanePolicyResponse(
                item.laneCode(),
                item.laneKind(),
                item.nominationRole(),
                item.funcaoOperacional(),
                item.processProfile(),
                item.displayName(),
                item.requiresGovBrRootIdentity(),
                item.minimumGovBrLevel(),
                item.allowsInstitutionManagedLogin(),
                item.requiresInstitutionManagedLogin(),
                item.requiresMfaAtEntry(),
                item.requiresQualifiedCertificateForEntry(),
                item.requiresQualifiedCertificateForSensitiveActs(),
                item.requiresInstitutionalNetwork(),
                item.allowsRemoteAuthorizedCertificate(),
                item.signsOrSubmitsSensitiveActs(),
                item.capacidades(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalAuthenticationPolicyResponse toResponse(InstitutionalAuthenticationPolicyClosure item) {
        return new NationalCommunicationInstitutionalAuthenticationPolicyResponse(
                item.affiliationId(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.organizationScope(),
                item.blueprintCode(),
                item.personalRootIdentityRequired(),
                item.managedInstitutionalLoginSupported(),
                item.managedInstitutionalLoginRequiresGovBrBinding(),
                item.dualEvidenceRequiredForSensitiveActs(),
                item.qualifiedCertificateRequiredForSigners(),
                item.trustedNetworkOrRemoteAuthorizationRequiredForCertificates(),
                item.lanePolicies().stream().map(this::toResponse).toList(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalOnboardingStepResponse toResponse(InstitutionalOnboardingStep item) {
        return new NationalCommunicationInstitutionalOnboardingStepResponse(
                item.stepCode(),
                item.title(),
                item.owner(),
                item.blocking(),
                item.requiredArtifacts(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalOnboardingPlanResponse toResponse(InstitutionalAffiliationOnboardingPlan item) {
        return new NationalCommunicationInstitutionalOnboardingPlanResponse(
                item.affiliationId(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.organizationScope(),
                item.blueprintCode(),
                item.coverageMode(),
                item.responsibleUnitCode(),
                item.responsibleUnitName(),
                item.selfServiceInstitutionManagedUsers(),
                item.govBrRootIdentityRequired(),
                item.signerDualEvidenceRequired(),
                item.dualAdministrationApprovalRequired(),
                item.steps().stream().map(this::toResponse).toList(),
                item.lanePolicies().stream().map(this::toResponse).toList(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalProvisionedDirectoryEntryResponse toResponse(InstitutionalProvisionedDirectoryEntry item) {
        return new NationalCommunicationInstitutionalProvisionedDirectoryEntryResponse(
                item.entryId(),
                item.entryType(),
                item.parentEntryId(),
                item.code(),
                item.displayName(),
                item.organizationalScope(),
                item.territorialScope(),
                item.caixaCodigo(),
                item.userId(),
                item.userName(),
                item.horizontalDataPlaneKey(),
                item.primaryWritePartitionKey(),
                item.readReplicaCode(),
                item.active(),
                item.findings(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalOperationalProvisioningResponse toResponse(InstitutionalOperationalProvisioningSnapshot item) {
        return new NationalCommunicationInstitutionalOperationalProvisioningResponse(
                item.provisioningId(),
                item.affiliationId(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.organizationScope(),
                item.blueprintCode(),
                item.status(),
                item.affiliationActive(),
                item.rootApprovalRequired(),
                item.rootApprovalSatisfied(),
                item.managedCredentialLaneSupported(),
                item.managedCredentialLaneReady(),
                item.trustedSignerLanePresent(),
                item.totalEntries(),
                item.totalCaixas(),
                item.totalLotacoes(),
                item.totalManagedCredentials(),
                item.entries().stream().map(this::toResponse).toList(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalManagedCredentialResponse toResponse(InstitutionalManagedCredential item) {
        return new NationalCommunicationInstitutionalManagedCredentialResponse(
                item.credentialId(),
                item.affiliationId(),
                item.nominationId(),
                item.nominatedUserId(),
                item.nominatedUserName(),
                item.managedUsername(),
                item.displayName(),
                item.laneCode(),
                item.signerOrSensitive(),
                item.allowsInstitutionManagedLogin(),
                item.govBrBindingRequired(),
                item.govBrBindingConfirmed(),
                item.status(),
                item.rotationWindowDays(),
                item.allowedNetworks(),
                item.findings(),
                item.fundamentos(),
                item.createdAt(),
                item.updatedAt());
    }

    public NationalCommunicationInstitutionalRootAdministratorApprovalResponse toResponse(InstitutionalRootAdministratorApproval item) {
        return new NationalCommunicationInstitutionalRootAdministratorApprovalResponse(
                item.approvalId(),
                item.affiliationId(),
                item.candidateUserId(),
                item.candidateUserName(),
                item.institutionActorUserId(),
                item.institutionActorName(),
                item.institutionApproved(),
                item.institutionApprovedAt(),
                item.pjbActorUserId(),
                item.pjbActorName(),
                item.pjbApproved(),
                item.pjbApprovedAt(),
                item.requiresDualApproval(),
                item.approved(),
                item.rejected(),
                item.findings(),
                item.fundamentos(),
                item.createdAt(),
                item.updatedAt());
    }

    public NationalCommunicationInstitutionalStrongSignaturePolicyResponse toResponse(InstitutionalStrongSignaturePolicy item) {
        return new NationalCommunicationInstitutionalStrongSignaturePolicyResponse(
                item.affiliationId(),
                item.nominationId(),
                item.userId(),
                item.userName(),
                item.laneCode(),
                item.signOrSubmitCapability(),
                item.managedCredentialActive(),
                item.govBrRequired(),
                item.govBrSatisfied(),
                item.govBrPrataOuroRequired(),
                item.govBrPrataOuroSatisfied(),
                item.qualifiedCertificateRequired(),
                item.qualifiedCertificateSatisfied(),
                item.trustedNetworkOrRemoteAuthorizationRequired(),
                item.trustedNetworkOrRemoteAuthorizationSatisfied(),
                item.mfaRequired(),
                item.mfaSatisfied(),
                item.rootAdministrationApprovalRequired(),
                item.rootAdministrationApprovalSatisfied(),
                item.allowed(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalManagedUnitResponse toResponse(InstitutionalManagedUnitEntry item) {
        return new NationalCommunicationInstitutionalManagedUnitResponse(
                item.unitCode(),
                item.unitName(),
                item.parentUnitCode(),
                item.territorialScope(),
                item.municipalityCoverage(),
                item.defaultBoxCode(),
                item.workPartition(),
                item.readReplicaCode(),
                item.managed(),
                item.homologated(),
                item.boxes(),
                item.laneCodes(),
                item.findings());
    }

    public NationalCommunicationInstitutionalLotationGovernanceResponse toResponse(InstitutionalLotationGovernanceEntry item) {
        return new NationalCommunicationInstitutionalLotationGovernanceResponse(
                item.lotationId(),
                item.nominationId(),
                item.userId(),
                item.userName(),
                item.unitCode(),
                item.boxCode(),
                item.laneCode(),
                item.nominationRole(),
                item.operationalFunction(),
                item.trustFloor(),
                item.active(),
                item.activeFrom(),
                item.activeUntil(),
                item.findings());
    }

    public NationalCommunicationInstitutionalUnitGovernanceResponse toResponse(InstitutionalUnitGovernanceSnapshot item) {
        return new NationalCommunicationInstitutionalUnitGovernanceResponse(
                item.snapshotId(),
                item.affiliationId(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.organizationScope(),
                item.status(),
                item.totalUnits(),
                item.totalBoxes(),
                item.totalLotacoes(),
                item.units().stream().map(this::toResponse).toList(),
                item.lotacoes().stream().map(this::toResponse).toList(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalWorkloadIdentityBindingResponse toResponse(InstitutionalWorkloadIdentityBinding item) {
        return new NationalCommunicationInstitutionalWorkloadIdentityBindingResponse(
                item.workloadCode(),
                item.displayName(),
                item.spiffeId(),
                item.serviceAccount(),
                item.namespace(),
                item.audience(),
                item.mtlsRequired(),
                item.projectedTokenRequired(),
                item.egressPolicies(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalWorkloadIdentityPlanResponse toResponse(InstitutionalWorkloadIdentityPlan item) {
        return new NationalCommunicationInstitutionalWorkloadIdentityPlanResponse(
                item.affiliationId(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.trustDomain(),
                item.namespace(),
                item.enabled(),
                item.mtlsRequired(),
                item.projectedServiceAccountTokenRequired(),
                item.workloads().stream().map(this::toResponse).toList(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalCoverageDelegationResponse toResponse(InstitutionalCoverageDelegationSnapshot item) {
        return new NationalCommunicationInstitutionalCoverageDelegationResponse(
                item.snapshotId(),
                item.affiliationId(),
                item.status(),
                item.totalDelegations(),
                item.activeDelegations(),
                item.delegations().stream().map(this::toResponse).toList(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalCoverageDelegationEntryResponse toResponse(InstitutionalCoverageDelegationEntry item) {
        return new NationalCommunicationInstitutionalCoverageDelegationEntryResponse(
                item.delegationId(),
                item.sourceLotationId(),
                item.sourceUserId(),
                item.sourceUserName(),
                item.targetLotationId(),
                item.targetUserId(),
                item.targetUserName(),
                item.unitCode(),
                item.boxCode(),
                item.laneCode(),
                item.delegationKind(),
                item.activeFrom(),
                item.activeUntil(),
                item.active(),
                item.crossMunicipalitySupport(),
                item.findings());
    }

    public NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse toResponse(InstitutionalApiEdgeSecurityProfile item) {
        return new NationalCommunicationInstitutionalApiEdgeSecurityProfileResponse(
                item.affiliationId(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.trustDomain(),
                item.gatewayClassName(),
                item.routeHostname(),
                item.gatewayApiManaged(),
                item.fapi2SecurityProfileRequired(),
                item.fapi2MessageSigningRequired(),
                item.senderConstrainedTokensRequired(),
                item.privateKeyJwtRequired(),
                item.pushedAuthorizationRequestsRequired(),
                item.pkceRequired(),
                item.mutualTlsRequired(),
                item.backendTlsPolicyRequired(),
                item.spiffeBindingRequired(),
                item.dpopAllowed(),
                item.recommendedCredentialRotationDays(),
                item.workloadBindings(),
                item.integrationFamilies(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalRecertificationResponse toResponse(InstitutionalRecertificationCycle item) {
        return new NationalCommunicationInstitutionalRecertificationResponse(
                item.affiliationId(),
                item.organizationScope(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.status(),
                item.totalAdministrators(),
                item.activeAdministrators(),
                item.totalActiveNominations(),
                item.dualAdministrationRequired(),
                item.dualAdministrationSatisfied(),
                item.dueNow(),
                item.compliant(),
                item.lastVerifiedAt(),
                item.nextDueAt(),
                item.pendingIssues(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalRevocationResponse toResponse(InstitutionalRevocationResult item) {
        return new NationalCommunicationInstitutionalRevocationResponse(
                item.affiliationId(),
                item.orgaoSigla(),
                item.unidadeCodigo(),
                item.targetedUserId(),
                item.targetedUnitCode(),
                item.revokeAffiliation(),
                item.resultingAffiliationStatus(),
                item.nominationsRevoked(),
                item.remainingActiveNominations(),
                item.remainingActiveAdministrators(),
                item.contextCutImmediately(),
                item.revokedNominationIds(),
                item.fundamentos(),
                item.processedAt());
    }

    public NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse toResponse(InstitutionalIntegrationSecurityPolicy item) {
        return new NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse(
                item.targetCode(),
                item.targetType(),
                item.organizationScope(),
                item.displayName(),
                item.trustFloor(),
                item.enabledChannels(),
                item.integrationFamilies(),
                item.requiresMutualTls(),
                item.requiresPayloadSignature(),
                item.requiresOriginAllowlist(),
                item.requiresImmediateRevocation(),
                item.requiresHumanApproval(),
                item.credentialRotationDays(),
                item.mandatoryControls(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalAffiliationValidationReportResponse toResponse(InstitutionalAffiliationValidationReport item) {
        return new NationalCommunicationInstitutionalAffiliationValidationReportResponse(
                item.validationId(),
                item.requestId(),
                item.organizationScope(),
                item.orgaoSigla(),
                item.unidadeCodigo(),
                item.aptaParaHomologacao(),
                item.documentosObrigatoriosPresentes(),
                item.representanteValidado(),
                item.dominioInstitucionalValidado(),
                item.certificadoMaterialValidado(),
                item.cadeiaConfiancaValidada(),
                item.findings().stream().map(this::toResponse).toList(),
                item.fundamentos(),
                item.validatedAt());
    }

    public NationalCommunicationInstitutionalAffiliationValidationFindingResponse toResponse(InstitutionalAffiliationValidationFinding item) {
        return new NationalCommunicationInstitutionalAffiliationValidationFindingResponse(
                item.code(),
                item.severity().name(),
                item.blocking(),
                item.message(),
                item.evidences());
    }

    public NationalCommunicationInstitutionalApprovalTrailResponse toResponse(InstitutionalAffiliationApprovalTrail item) {
        return new NationalCommunicationInstitutionalApprovalTrailResponse(
                item.trailId(),
                item.requestId(),
                item.representativeUserId(),
                item.representativeName(),
                item.representativeSigned(),
                item.representativeSignedAt(),
                item.pjbApproverUserId(),
                item.pjbApproverName(),
                item.approvedByPjb(),
                item.pjbDecidedAt(),
                item.dualKeySatisfied(),
                item.currentStatus(),
                item.fundamentos(),
                item.updatedAt());
    }

    public NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse toResponse(InstitutionalRemoteCertificateAuthorization item) {
        return new NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse(
                item.authorizationId(),
                item.affiliationId(),
                item.nominatedUserId(),
                item.issuedByUserId(),
                item.issuedByUserName(),
                item.reason(),
                item.allowedNetworks(),
                item.allowedDevices(),
                item.validFrom(),
                item.validUntil(),
                item.status().name(),
                item.fundamentos(),
                item.createdAt(),
                item.updatedAt());
    }

    public NationalCommunicationInstitutionalSessionRiskAssessmentResponse toResponse(InstitutionalSessionRiskAssessment item) {
        return new NationalCommunicationInstitutionalSessionRiskAssessmentResponse(
                item.assessmentId(),
                item.userId(),
                item.userName(),
                item.affiliationId(),
                item.nominationId(),
                item.unidadeCodigo(),
                item.caixaCodigo(),
                item.deviceId(),
                item.ipAddress(),
                item.geographicUf(),
                item.riskScore(),
                item.riskLevel(),
                item.requiresStepUp(),
                item.requiresManualApproval(),
                item.blocked(),
                item.findings().stream().map(this::toResponse).toList(),
                item.fundamentos(),
                item.assessedAt());
    }

    public NationalCommunicationInstitutionalSessionRiskFindingResponse toResponse(InstitutionalSessionRiskFinding item) {
        return new NationalCommunicationInstitutionalSessionRiskFindingResponse(
                item.code(),
                item.severity().name(),
                item.blocking(),
                item.message(),
                item.evidences());
    }

    public NationalCommunicationInstitutionalSensitiveActAuthorizationResponse toResponse(InstitutionalSensitiveActAuthorization item) {
        return new NationalCommunicationInstitutionalSensitiveActAuthorizationResponse(
                item.authorizationId(),
                item.sensitiveAct().name(),
                item.userId(),
                item.userName(),
                item.affiliationId(),
                item.nominationId(),
                item.achievedTrust() == null ? null : item.achievedTrust().name(),
                item.requiredTrust() == null ? null : item.requiredTrust().name(),
                item.allowed(),
                item.requiresManualApproval(),
                item.blocked(),
                item.findings(),
                item.fundamentos(),
                item.evaluatedAt());
    }

    public NationalCommunicationInstitutionalIntegrationCredentialResponse toResponse(InstitutionalIntegrationCredentialApplicationService.IssuedCredential issued) {
        return toResponse(issued.credential(), issued.plaintextSecret());
    }

    public NationalCommunicationInstitutionalIntegrationCredentialResponse toResponse(InstitutionalIntegrationCredential item, String plaintextSecret) {
        return new NationalCommunicationInstitutionalIntegrationCredentialResponse(
                item.credentialId(),
                item.affiliationId(),
                item.displayName(),
                item.integrationFamilies(),
                item.originAllowlist(),
                item.requiresPayloadSignature(),
                item.requiresMutualTls(),
                item.requiresHumanApproval(),
                item.requiresImmediateRevocation(),
                item.credentialRotationDays(),
                item.status().name(),
                item.keyId(),
                item.secretPreview(),
                plaintextSecret,
                item.issuedAt(),
                item.rotatedAt(),
                item.expiresAt(),
                item.revokedAt(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalIntegrationCallTrailResponse toResponse(InstitutionalIntegrationCallTrail item) {
        return new NationalCommunicationInstitutionalIntegrationCallTrailResponse(
                item.trailId(),
                item.credentialId(),
                item.correlationId(),
                item.origin(),
                item.payloadDigest(),
                item.payloadSignaturePresent(),
                item.idempotencyKey(),
                item.resultCode(),
                item.findings(),
                item.calledAt());
    }
}