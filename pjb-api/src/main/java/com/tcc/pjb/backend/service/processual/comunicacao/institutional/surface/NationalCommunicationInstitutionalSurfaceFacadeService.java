package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalBindingApprovalApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalContextActivationGuardApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalIdentityGuardApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalRepresentativeVerificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalStepUpAuthenticationPolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalTextClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalDelegatedAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOfficialSourceDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalPublicRecognitionGateApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalTrustMatrixApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationDocument;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustMatrixEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.application.InstitutionalCanonicalCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalDelegatedGovernanceClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalOperatingModelClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingAdministrativeSeat;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingCoverageRoute;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingModelClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingRoleBand;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.application.InstitutionalProceduralCoherenceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralActEvaluation;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceAggregate;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCompetenceEnvelope;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralContextVector;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralNextBestAct;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryActivationDecisionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationBundle;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialIdentifierDossierApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceAttestationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierCheck;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialIdentifierDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestationItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceDossier;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceEvidence;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.application.InstitutionalIntegrationContractCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessActionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessAuthorityBand;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessDiagnosticFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessQueueSectionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessSeparatorSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessVisualLaneSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspaceSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalEntryGuardApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalLifecycleApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalStructuralDiagnosticApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalEntryGuardSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalFourLevelAccessSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalCaseSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalLifecycle;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.application.InstitutionalRecipientTopologyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalInboxBatchApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalNoReadCertificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalNoticeChannelApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalOperationalCoverageApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalPanelApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalSlaPredictiveApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application.InstitutionalTriageSuggestionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalOperationalCoverageRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalTriageSuggestionDashboard;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.NationalCommunicationInstitutionalTopologyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalFourLevelAccessResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalIdentityGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalRepresentativeVerificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationDocumentRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationRequestResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegatedAffiliationCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalCanonicalCatalogEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntrySummaryResponse;
import com.tcc.pjb.backend.model.dto.security.context.PjbAuthenticatedSessionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryActivationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalTextClosureAuditResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBindingApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBootstrapAdministratorRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageApplyRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalSlaPredictiveDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStepUpPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustMatrixEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalBulkRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalBulkResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalContextActivationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalNoReadRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalCaseResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalLifecycleResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalInboxItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelSummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTriageSuggestionDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalUnitQueueResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralActEvaluationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.InstitutionalProceduralCoherenceReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessDiagnosticReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationContractDescriptorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierCheckResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceEvidenceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceRevalidationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalNoticeChannelResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOperatingModelResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalStructuralDiagnosticResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundle;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import com.tcc.pjb.backend.service.security.context.PjbAuthenticatedSessionFacadeService;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NationalCommunicationInstitutionalSurfaceFacadeService {

    private final InstitutionalRepresentativeVerificationApplicationService representativeVerificationApplicationService;
    private final InstitutionalBindingApprovalApplicationService bindingApprovalApplicationService;
    private final InstitutionalIdentityGuardApplicationService identityGuardApplicationService;
    private final InstitutionalStepUpAuthenticationPolicyApplicationService stepUpAuthenticationPolicyApplicationService;
    private final InstitutionalContextActivationGuardApplicationService contextActivationGuardApplicationService;
    private final InstitutionalTextClosureApplicationService textClosureApplicationService;
    private final InstitutionalCanonicalCatalogApplicationService canonicalCatalogService;
    private final InstitutionalSlaPredictiveApplicationService predictiveService;
    private final InstitutionalInboxBatchApplicationService batchService;
    private final InstitutionalTriageSuggestionApplicationService triageSuggestionService;
    private final InstitutionalIntegrationContractCatalogApplicationService contractCatalogService;
    private final InstitutionalOperationalCoverageApplicationService coverageService;
    private final InstitutionalPanelApplicationService panelService;
    private final InstitutionalNoticeChannelApplicationService noticeChannelService;
    private final InstitutionalNoReadCertificationApplicationService noReadService;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;
    private final InstitutionalEntryActivationDecisionApplicationService entryActivationDecisionApplicationService;
    private final InstitutionalOperationalLifecycleApplicationService lifecycleApplicationService;
    private final InstitutionalEntryGuardApplicationService entryGuardApplicationService;
    private final InstitutionalOperationalClosureApplicationService operationalClosureApplicationService;
    private final InstitutionalStructuralDiagnosticApplicationService structuralDiagnosticApplicationService;
    private final InstitutionalRecipientTopologyApplicationService topologyApplicationService;
    private final InstitutionalProcessWorkspaceApplicationService processWorkspaceApplicationService;
    private final InstitutionalProceduralCoherenceApplicationService proceduralCoherenceApplicationService;
    private final InstitutionalDelegatedGovernanceClosureApplicationService delegatedGovernanceClosureApplicationService;
    private final InstitutionalAffiliationApplicationService affiliationApplicationService;
    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService;
    private final InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService;
    private final InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService;
    private final InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService;
    private final InstitutionalOperatingModelClosureApplicationService operatingModelClosureApplicationService;
    private final InstitutionalDelegatedAffiliationApplicationService delegatedAffiliationApplicationService;
    private final InstitutionalTrustMatrixApplicationService trustMatrixApplicationService;
    private final InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService;
    private final NationalCommunicationInstitutionalFacadeSupport facadeSupport;
    private final NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService;
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport;
    private final PjbAuthenticatedSessionFacadeService authenticatedSessionFacadeService;

    public NationalCommunicationInstitutionalSurfaceFacadeService(
            InstitutionalRepresentativeVerificationApplicationService representativeVerificationApplicationService,
            InstitutionalBindingApprovalApplicationService bindingApprovalApplicationService,
            InstitutionalIdentityGuardApplicationService identityGuardApplicationService,
            InstitutionalStepUpAuthenticationPolicyApplicationService stepUpAuthenticationPolicyApplicationService,
            InstitutionalContextActivationGuardApplicationService contextActivationGuardApplicationService,
            InstitutionalTextClosureApplicationService textClosureApplicationService,
            InstitutionalCanonicalCatalogApplicationService canonicalCatalogService,
            InstitutionalSlaPredictiveApplicationService predictiveService,
            InstitutionalInboxBatchApplicationService batchService,
            InstitutionalTriageSuggestionApplicationService triageSuggestionService,
            InstitutionalIntegrationContractCatalogApplicationService contractCatalogService,
            InstitutionalOperationalCoverageApplicationService coverageService,
            InstitutionalPanelApplicationService panelService,
            InstitutionalNoticeChannelApplicationService noticeChannelService,
            InstitutionalNoReadCertificationApplicationService noReadService,
            InstitutionalEntryContextApplicationService entryContextApplicationService,
            InstitutionalEntryActivationDecisionApplicationService entryActivationDecisionApplicationService,
            InstitutionalOperationalLifecycleApplicationService lifecycleApplicationService,
            InstitutionalEntryGuardApplicationService entryGuardApplicationService,
            InstitutionalOperationalClosureApplicationService operationalClosureApplicationService,
            InstitutionalStructuralDiagnosticApplicationService structuralDiagnosticApplicationService,
            InstitutionalRecipientTopologyApplicationService topologyApplicationService,
            InstitutionalProcessWorkspaceApplicationService processWorkspaceApplicationService,
            InstitutionalProceduralCoherenceApplicationService proceduralCoherenceApplicationService,
            InstitutionalDelegatedGovernanceClosureApplicationService delegatedGovernanceClosureApplicationService,
            InstitutionalAffiliationApplicationService affiliationApplicationService,
            InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService,
            InstitutionalOfficialSourceDossierApplicationService officialSourceDossierApplicationService,
            InstitutionalOfficialIdentifierDossierApplicationService officialIdentifierDossierApplicationService,
            InstitutionalOfficialSourceAttestationApplicationService officialSourceAttestationApplicationService,
            InstitutionalOperatingModelClosureApplicationService operatingModelClosureApplicationService,
            InstitutionalDelegatedAffiliationApplicationService delegatedAffiliationApplicationService,
            InstitutionalTrustMatrixApplicationService trustMatrixApplicationService,
            InstitutionalPanelBlueprintApplicationService panelBlueprintApplicationService,
            NationalCommunicationInstitutionalFacadeSupport facadeSupport,
            NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService,
            NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport,
            PjbAuthenticatedSessionFacadeService authenticatedSessionFacadeService) {
        this.representativeVerificationApplicationService = representativeVerificationApplicationService;
        this.bindingApprovalApplicationService = bindingApprovalApplicationService;
        this.identityGuardApplicationService = identityGuardApplicationService;
        this.stepUpAuthenticationPolicyApplicationService = stepUpAuthenticationPolicyApplicationService;
        this.contextActivationGuardApplicationService = contextActivationGuardApplicationService;
        this.textClosureApplicationService = textClosureApplicationService;
        this.canonicalCatalogService = canonicalCatalogService;
        this.predictiveService = predictiveService;
        this.batchService = batchService;
        this.triageSuggestionService = triageSuggestionService;
        this.contractCatalogService = contractCatalogService;
        this.coverageService = coverageService;
        this.panelService = panelService;
        this.noticeChannelService = noticeChannelService;
        this.noReadService = noReadService;
        this.entryContextApplicationService = entryContextApplicationService;
        this.entryActivationDecisionApplicationService = entryActivationDecisionApplicationService;
        this.lifecycleApplicationService = lifecycleApplicationService;
        this.entryGuardApplicationService = entryGuardApplicationService;
        this.operationalClosureApplicationService = operationalClosureApplicationService;
        this.structuralDiagnosticApplicationService = structuralDiagnosticApplicationService;
        this.topologyApplicationService = topologyApplicationService;
        this.processWorkspaceApplicationService = processWorkspaceApplicationService;
        this.proceduralCoherenceApplicationService = proceduralCoherenceApplicationService;
        this.delegatedGovernanceClosureApplicationService = delegatedGovernanceClosureApplicationService;
        this.affiliationApplicationService = affiliationApplicationService;
        this.publicRecognitionGateApplicationService = publicRecognitionGateApplicationService;
        this.officialSourceDossierApplicationService = officialSourceDossierApplicationService;
        this.officialIdentifierDossierApplicationService = officialIdentifierDossierApplicationService;
        this.officialSourceAttestationApplicationService = officialSourceAttestationApplicationService;
        this.operatingModelClosureApplicationService = operatingModelClosureApplicationService;
        this.delegatedAffiliationApplicationService = delegatedAffiliationApplicationService;
        this.trustMatrixApplicationService = trustMatrixApplicationService;
        this.panelBlueprintApplicationService = panelBlueprintApplicationService;
        this.facadeSupport = facadeSupport;
        this.stateBundleFacadeService = stateBundleFacadeService;
        this.surfaceAssemblerSupport = surfaceAssemblerSupport;
        this.authenticatedSessionFacadeService = authenticatedSessionFacadeService;
    }

    public Optional<NationalCommunicationInstitutionalRepresentativeVerificationResponse> verificarRepresentante(String requestId) {
        return representativeVerificationApplicationService.buscarSeExistir(requestId).map(surfaceAssemblerSupport::toResponse);
    }

    public NationalCommunicationInstitutionalBindingApprovalResponse aprovacaoVinculo(String affiliationId, String nominationId) {
        return surfaceAssemblerSupport.toResponse(bindingApprovalApplicationService.avaliarAtual(affiliationId, nominationId));
    }

    public NationalCommunicationInstitutionalIdentityGuardResponse guardaIdentidade() {
        return surfaceAssemblerSupport.toResponse(identityGuardApplicationService.avaliarAtual());
    }

    public NationalCommunicationInstitutionalStepUpPolicyResponse politicaStepUp(String affiliationId, String nominationId, String sensitiveAct) {
        return surfaceAssemblerSupport.toResponse(stepUpAuthenticationPolicyApplicationService.avaliarAtual(affiliationId, nominationId, sensitiveAct));
    }

    public NationalCommunicationInstitutionalContextActivationResponse ativacaoContexto(String affiliationId, String nominationId, String unidadeCodigo, String caixaCodigo, String sensitiveAct) {
        return surfaceAssemblerSupport.toResponse(contextActivationGuardApplicationService.avaliarAtual(affiliationId, nominationId, unidadeCodigo, caixaCodigo, sensitiveAct));
    }

    public NationalCommunicationInstitutionalTextClosureAuditResponse fechamentoTexto() {
        return surfaceAssemblerSupport.toResponse(textClosureApplicationService.auditar());
    }

    public List<NationalCommunicationInstitutionalCanonicalCatalogEntryResponse> catalogoCanonico() {
        return canonicalCatalogService.list().stream().map(surfaceAssemblerSupport::toCanonicalCatalogEntry).toList();
    }

    public NationalCommunicationInstitutionalSlaPredictiveDashboardResponse slaPreditivo(String uf, DestinatarioInstitucionalKind destinatarioKind) {
        return surfaceAssemblerSupport.toSlaDashboard(predictiveService.dashboard(uf, destinatarioKind));
    }

    public NationalCommunicationInstitutionalBulkResponse receberLote(NationalCommunicationInstitutionalBulkRequest request) {
        return surfaceAssemblerSupport.toBulkResponse(batchService.receberLote(request.expedicoesUuids(), request.detalhe()));
    }

    public NationalCommunicationInstitutionalBulkResponse certificarCienciaLote(NationalCommunicationInstitutionalBulkRequest request) {
        return surfaceAssemblerSupport.toBulkResponse(batchService.certificarCienciaLote(request.expedicoesUuids(), request.detalhe()));
    }

    public NationalCommunicationInstitutionalTriageSuggestionDashboardResponse triagemSugerida(String expedicaoUuid) {
        return surfaceAssemblerSupport.toTriageDashboard(triageSuggestionService.suggest(expedicaoUuid));
    }

    public List<NationalCommunicationInstitutionalIntegrationContractDescriptorResponse> contratoIntegracao() {
        return contractCatalogService.list().stream().map(surfaceAssemblerSupport::toIntegrationContract).toList();
    }

    public List<NationalCommunicationInstitutionalCoverageResponse> coberturasOperacionais(String unidadeCodigo) {
        return coverageService.listar(unidadeCodigo).stream().map(surfaceAssemblerSupport::toCoverage).toList();
    }

    public List<NationalCommunicationInstitutionalDelegationResponse> aplicarCoberturasAtivas(NationalCommunicationInstitutionalCoverageApplyRequest request) {
        return coverageService.aplicarAtivas(request.expedicaoUuid(), request.motivoComplementar()).stream().map(surfaceAssemblerSupport::toDelegation).toList();
    }

    public List<NationalCommunicationInstitutionalPanelSummaryResponse> painelOrgao(String unidadeCodigo) {
        return panelService.painelOrgao(unidadeCodigo).stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    public List<NationalCommunicationInstitutionalUnitQueueResponse> filasUnidade(String unidadeCodigo) {
        return panelService.filasUnidade(unidadeCodigo).stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    public List<NationalCommunicationInstitutionalNoticeChannelResponse> avisosExternos() {
        return noticeChannelService.list().stream().map(surfaceAssemblerSupport::toNoticeChannel).toList();
    }

    public List<NationalCommunicationInstitutionalInboxItemResponse> pendentesNaoLeitura(String unidadeCodigo) {
        return noReadService.pendentesDecurso(unidadeCodigo).stream().map(surfaceAssemblerSupport::toInbox).toList();
    }

    public NationalCommunicationInstitutionalActionResponse certificarNaoLeitura(NationalCommunicationInstitutionalNoReadRequest request) {
        return surfaceAssemblerSupport.toActionResponse(noReadService.certificarNaoLeitura(request.expedicaoUuid(), request.detalhe()));
    }

    public NationalCommunicationInstitutionalEntrySummaryResponse entradaInteligente() {
        InstitutionalEntrySummary summary = entryContextApplicationService.resolverEntradaAtual();
        NationalCommunicationInstitutionalStateBundle stateBundle = stateBundleFacadeService.carregar(summary);
        InstitutionalEntryActivationBundle activationBundle = entryActivationDecisionApplicationService.avaliarEntradaAtual(summary);
        InstitutionalOperationalProfileProjection operationalProfile = stateBundle.operationalProfile();
        InstitutionalEntryActivationDecision activationDecision = activationBundle.decision();
        PjbAuthenticatedSessionResponse authenticatedSession = authenticatedSessionFacadeService.atual(summary, stateBundle);
        return new NationalCommunicationInstitutionalEntrySummaryResponse(
                summary.usuarioId(),
                summary.nomeUsuario(),
                summary.tipoUsuario() == null ? null : summary.tipoUsuario().name(),
                surfaceAssemblerSupport.toIdentityBase(summary.identidadeBase()),
                summary.possuiAmbientePessoal(),
                summary.possuiAmbienteInstitucional(),
                summary.contextos().stream().map(surfaceAssemblerSupport::toContext).toList(),
                summary.contextoPreferencial() == null ? null : surfaceAssemblerSupport.toContext(summary.contextoPreferencial()),
                toOperationalProfile(operationalProfile),
                toActivation(activationDecision),
                authenticatedSession,
                summary.generatedAt()
        );
    }

    private NationalCommunicationInstitutionalOperationalProfileResponse toOperationalProfile(InstitutionalOperationalProfileProjection operationalProfile) {
        return surfaceAssemblerSupport.toOperationalProfile(operationalProfile);
    }

    private NationalCommunicationInstitutionalEntryActivationResponse toActivation(InstitutionalEntryActivationDecision activationDecision) {
        return surfaceAssemblerSupport.toActivation(activationDecision);
    }

    public List<NationalCommunicationInstitutionalEntryContextResponse> contextosEntrada() {
        return entryContextApplicationService.resolverContextosAtuais().stream().map(surfaceAssemblerSupport::toContext).toList();
    }

    public List<NationalCommunicationInstitutionalOperationalLifecycleResponse> cadastrosOperacionais() {
        return lifecycleApplicationService.listar().stream().map(surfaceAssemblerSupport::toLifecycle).toList();
    }

    public Optional<NationalCommunicationInstitutionalOperationalLifecycleResponse> detalharAfiliacaoLifecycle(String affiliationId) {
        return lifecycleApplicationService.detalharAfiliacao(affiliationId).map(surfaceAssemblerSupport::toLifecycle);
    }

    public Optional<NationalCommunicationInstitutionalOperationalLifecycleResponse> detalharSolicitacaoLifecycle(String requestId) {
        return lifecycleApplicationService.detalharSolicitacao(requestId).map(surfaceAssemblerSupport::toLifecycle);
    }

    public NationalCommunicationInstitutionalEntryGuardResponse guardiaoEntrada() {
        return surfaceAssemblerSupport.toGuard(entryGuardApplicationService.avaliarEntradaAtual());
    }

    public NationalCommunicationInstitutionalFourLevelAccessResponse quatroNiveis(String affiliationId) {
        return surfaceAssemblerSupport.toResponse(operationalClosureApplicationService.resolverQuatroNiveisAtual(affiliationId));
    }

    public List<NationalCommunicationInstitutionalOperationalCaseResponse> casosOperacionais(String affiliationId) {
        return operationalClosureApplicationService.listarCasosOperacionais(affiliationId).stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    public NationalCommunicationInstitutionalStructuralDiagnosticResponse diagnosticoEstrutural(String affiliationId) {
        return surfaceAssemblerSupport.toResponse(structuralDiagnosticApplicationService.diagnosticar(affiliationId));
    }

    public List<NationalCommunicationInstitutionalTopologyResponse> topologiaDestinatarios() {
        return topologyApplicationService.list().stream().map(surfaceAssemblerSupport::toTopology).toList();
    }

    public List<NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse> listarWorkspaces(Long processoId, String rito, String fase, String status, String ramo) {
        return processWorkspaceApplicationService.listarPerfis(processoId, rito, fase, status, ramo).stream().map(surfaceAssemblerSupport::toSummary).toList();
    }

    public NationalCommunicationInstitutionalProcessWorkspaceResponse detalharWorkspace(String profileCode, Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toResponse(processWorkspaceApplicationService.detalharPerfil(profileCode, processoId, rito, fase, status, ramo));
    }

    public NationalCommunicationInstitutionalProcessDiagnosticReportResponse diagnosticarWorkspace(Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toDiagnostic(processWorkspaceApplicationService.diagnosticar(processoId, rito, fase, status, ramo));
    }

    public InstitutionalProceduralCoherenceReportResponse diagnosticarCoerencia(Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toDiagnostic(proceduralCoherenceApplicationService.diagnosticar(processoId, rito, fase, status, ramo));
    }

    public NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse detalharCoerencia(String profileCode, Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toAggregate(proceduralCoherenceApplicationService.detalhar(profileCode, processoId, rito, fase, status, ramo));
    }

    public NationalCommunicationInstitutionalProceduralActEvaluationResponse avaliarAtoCoerencia(String profileCode, String actionCode, Long processoId, String rito, String fase, String status, String ramo) {
        return surfaceAssemblerSupport.toActEvaluation(proceduralCoherenceApplicationService.avaliarAto(profileCode, actionCode, processoId, rito, fase, status, ramo));
    }

    public NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse fechamentoDelegado(String scope) {
        return surfaceAssemblerSupport.toResponse(delegatedGovernanceClosureApplicationService.consolidar(scope));
    }

    public NationalCommunicationInstitutionalOperatingModelResponse modeloOperacional(String affiliationId,
                                                                                      String destinatarioKind,
                                                                                      String municipio,
                                                                                      String uf) {
        DestinatarioInstitucionalKind requestedKind = destinatarioKind == null || destinatarioKind.isBlank()
                ? null
                : facadeSupport.parseDestinatarioKind(destinatarioKind);
        InstitutionalAffiliation affiliation = affiliationApplicationService.listarAfiliacoes().stream()
                .filter(item -> affiliationId == null || affiliationId.isBlank() || item.affiliationId().equalsIgnoreCase(affiliationId.trim()))
                .sorted(Comparator.comparing(InstitutionalAffiliation::updatedAt).reversed())
                .findFirst()
                .orElse(null);
        List<InstitutionalNomination> nominations = affiliation == null
                ? List.of()
                : affiliationApplicationService.listarNomeacoes(null).stream()
                .filter(item -> item.affiliationId().equalsIgnoreCase(affiliation.affiliationId()))
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt).reversed())
                .toList();
        InstitutionalOperatingModelClosure closure = operatingModelClosureApplicationService.consolidar(
                affiliation,
                nominations,
                requestedKind,
                municipio,
                uf);
        return surfaceAssemblerSupport.toResponse(closure);
    }

    public NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse entradaAtualDelegada() {
        return surfaceAssemblerSupport.toResponse(delegatedGovernanceClosureApplicationService.entradaAtual());
    }

    public NationalCommunicationInstitutionalAffiliationRequestResponse solicitarAdesaoDelegada(NationalCommunicationInstitutionalDelegatedAffiliationCreateRequest request) {
        InstitutionalAffiliationRequest created = delegatedAffiliationApplicationService.solicitarAdesao(
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
                request.representativeName(),
                parseNominationRole(request.representativeRole()),
                toBootstrapMap(request.bootstrapAdministrators()),
                parseTrustLevel(request.trustFloor()),
                request.requerDuplaAprovacaoAdministrador(),
                request.requerCertificadoICP(),
                request.restringeCertificadoRedeInstitucional(),
                request.permiteUsoRemotoComAutorizacao(),
                request.canaisHabilitados(),
                request.politicaCiencia(),
                request.sla(),
                request.regrasFallback(),
                request.conveniosIntegracoes(),
                toDocuments(request.documentos()),
                request.fundamentos());
        return surfaceAssemblerSupport.toResponse(created);
    }

    public NationalCommunicationInstitutionalAffiliationRequestResponse homologarAdesaoDelegada(String requestId, NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest request) {
        return surfaceAssemblerSupport.toResponse(delegatedAffiliationApplicationService.homologarSolicitacao(requestId, request.aprovar(), request.fundamentos()));
    }

    public List<NationalCommunicationInstitutionalAffiliationRequestResponse> listarAdesoesDelegadas() {
        return delegatedAffiliationApplicationService.listarSolicitacoes().stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    public AdminInstitutionalPublicRecognitionResponse reconhecimentoPublicoAdesaoDelegada(String requestId) {
        return publicRecognitionGateApplicationService.avaliarSolicitacao(requestId);
    }

    public NationalCommunicationInstitutionalOfficialSourceDossierResponse dossieFontesOficiaisAdesaoDelegada(String requestId) {
        return toOfficialSourceDossierResponse(officialSourceDossierApplicationService.gerarSolicitacao(requestId));
    }

    public NationalCommunicationInstitutionalOfficialIdentifierDossierResponse identificadoresOficiaisAdesaoDelegada(String requestId) {
        return toOfficialIdentifierDossierResponse(officialIdentifierDossierApplicationService.gerarSolicitacao(requestId));
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse atestacaoFontesOficiaisAdesaoDelegada(String requestId) {
        return toOfficialSourceAttestationResponse(officialSourceAttestationApplicationService.consultarSolicitacao(requestId));
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse revalidarFontesOficiaisAdesaoDelegada(String requestId,
                                                                                                                       NationalCommunicationInstitutionalOfficialSourceRevalidationRequest request) {
        return toOfficialSourceAttestationResponse(officialSourceAttestationApplicationService.revalidarSolicitacao(
                requestId,
                request == null ? List.of() : request.fundamentos()));
    }

    public List<NationalCommunicationInstitutionalTrustMatrixEntryResponse> matrizConfiabilidade(String scope) {
        return trustMatrixApplicationService.listar(scope).stream().map(surfaceAssemblerSupport::toResponse).toList();
    }

    public List<NationalCommunicationInstitutionalPanelBlueprintResponse> painelBlueprints(String scope, String panel) {
        return panelBlueprintApplicationService.listar(scope, panel).stream().map(surfaceAssemblerSupport::toResponse).toList();
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

    private InstitutionalNominationRole parseNominationRole(String raw) {
        return facadeSupport.parseNominationRole(raw);
    }

    private InstitutionalTrustLevel parseTrustLevel(String raw) {
        return facadeSupport.parseTrustLevel(raw);
    }

    private Map<Long, String> toBootstrapMap(List<NationalCommunicationInstitutionalBootstrapAdministratorRequest> bootstrapAdministrators) {
        if (bootstrapAdministrators == null || bootstrapAdministrators.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<Long, String> out = new LinkedHashMap<>();
        for (NationalCommunicationInstitutionalBootstrapAdministratorRequest item : bootstrapAdministrators) {
            if (item != null && item.userId() != null) {
                out.put(item.userId(), item.userName());
            }
        }
        return Collections.unmodifiableMap(out);
    }

    private List<InstitutionalAffiliationDocument> toDocuments(List<NationalCommunicationInstitutionalAffiliationDocumentRequest> documentos) {
        if (documentos == null || documentos.isEmpty()) {
            return List.of();
        }
        return documentos.stream()
                .filter(item -> item != null && item.codigo() != null && !item.codigo().isBlank() && item.nome() != null && !item.nome().isBlank() && item.tipo() != null && !item.tipo().isBlank())
                .map(item -> new InstitutionalAffiliationDocument(
                        item.codigo().trim(),
                        item.nome().trim(),
                        item.tipo().trim(),
                        item.referenciaExterna(),
                        item.hashDocumento(),
                        item.obrigatorio() != null && item.obrigatorio(),
                        item.validado() != null && item.validado()))
                .toList();
    }

}