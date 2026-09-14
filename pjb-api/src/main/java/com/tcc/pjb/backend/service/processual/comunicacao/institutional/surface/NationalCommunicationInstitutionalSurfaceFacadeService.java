package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalDelegatedAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationDocument;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustMatrixEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalOperatingModelClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingAdministrativeSeat;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingCoverageRoute;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingModelClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingRoleBand;
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
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessActionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessAuthorityBand;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessDiagnosticFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessQueueSectionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessSeparatorSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessVisualLaneSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspaceSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalEntryGuardSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalFourLevelAccessSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalCaseSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalLifecycle;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticReport;
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
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceDossierResponse;
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

    private final NationalCommunicationInstitutionalAccessGuardSurfaceService accessGuard;
    private final NationalCommunicationInstitutionalCatalogDescriptorsSurfaceService catalogDescriptors;
    private final NationalCommunicationInstitutionalWorkflowOperationsSurfaceService workflowOperations;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;
    private final InstitutionalEntryActivationDecisionApplicationService entryActivationDecisionApplicationService;
    private final NationalCommunicationInstitutionalEntryLifecycleSurfaceService entryLifecycle;
    private final NationalCommunicationInstitutionalProcessWorkspaceSurfaceService processWorkspace;
    private final NationalCommunicationInstitutionalAffiliationGovernanceSurfaceService affiliationGovernance;
    private final InstitutionalAffiliationApplicationService affiliationApplicationService;
    private final InstitutionalOperatingModelClosureApplicationService operatingModelClosureApplicationService;
    private final InstitutionalDelegatedAffiliationApplicationService delegatedAffiliationApplicationService;
    private final NationalCommunicationInstitutionalFacadeSupport facadeSupport;
    private final NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService;
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport;
    private final PjbAuthenticatedSessionFacadeService authenticatedSessionFacadeService;

    public NationalCommunicationInstitutionalSurfaceFacadeService(
            NationalCommunicationInstitutionalAccessGuardSurfaceService accessGuard,
            NationalCommunicationInstitutionalCatalogDescriptorsSurfaceService catalogDescriptors,
            NationalCommunicationInstitutionalWorkflowOperationsSurfaceService workflowOperations,
            InstitutionalEntryContextApplicationService entryContextApplicationService,
            InstitutionalEntryActivationDecisionApplicationService entryActivationDecisionApplicationService,
            NationalCommunicationInstitutionalEntryLifecycleSurfaceService entryLifecycle,
            NationalCommunicationInstitutionalProcessWorkspaceSurfaceService processWorkspace,
            NationalCommunicationInstitutionalAffiliationGovernanceSurfaceService affiliationGovernance,
            InstitutionalAffiliationApplicationService affiliationApplicationService,
            InstitutionalOperatingModelClosureApplicationService operatingModelClosureApplicationService,
            InstitutionalDelegatedAffiliationApplicationService delegatedAffiliationApplicationService,
            NationalCommunicationInstitutionalFacadeSupport facadeSupport,
            NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService,
            NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport,
            PjbAuthenticatedSessionFacadeService authenticatedSessionFacadeService) {
        this.accessGuard = accessGuard;
        this.catalogDescriptors = catalogDescriptors;
        this.workflowOperations = workflowOperations;
        this.entryContextApplicationService = entryContextApplicationService;
        this.entryActivationDecisionApplicationService = entryActivationDecisionApplicationService;
        this.entryLifecycle = entryLifecycle;
        this.processWorkspace = processWorkspace;
        this.affiliationGovernance = affiliationGovernance;
        this.affiliationApplicationService = affiliationApplicationService;
        this.operatingModelClosureApplicationService = operatingModelClosureApplicationService;
        this.delegatedAffiliationApplicationService = delegatedAffiliationApplicationService;
        this.facadeSupport = facadeSupport;
        this.stateBundleFacadeService = stateBundleFacadeService;
        this.surfaceAssemblerSupport = surfaceAssemblerSupport;
        this.authenticatedSessionFacadeService = authenticatedSessionFacadeService;
    }

    public Optional<NationalCommunicationInstitutionalRepresentativeVerificationResponse> verificarRepresentante(String requestId) {
        return accessGuard.verificarRepresentante(requestId);
    }

    public NationalCommunicationInstitutionalBindingApprovalResponse aprovacaoVinculo(String affiliationId, String nominationId) {
        return accessGuard.aprovacaoVinculo(affiliationId, nominationId);
    }

    public NationalCommunicationInstitutionalIdentityGuardResponse guardaIdentidade() {
        return accessGuard.guardaIdentidade();
    }

    public NationalCommunicationInstitutionalStepUpPolicyResponse politicaStepUp(String affiliationId, String nominationId, String sensitiveAct) {
        return accessGuard.politicaStepUp(affiliationId, nominationId, sensitiveAct);
    }

    public NationalCommunicationInstitutionalContextActivationResponse ativacaoContexto(String affiliationId, String nominationId, String unidadeCodigo, String caixaCodigo, String sensitiveAct) {
        return accessGuard.ativacaoContexto(affiliationId, nominationId, unidadeCodigo, caixaCodigo, sensitiveAct);
    }

    public NationalCommunicationInstitutionalTextClosureAuditResponse fechamentoTexto() {
        return accessGuard.fechamentoTexto();
    }

    public List<NationalCommunicationInstitutionalCanonicalCatalogEntryResponse> catalogoCanonico() {
        return catalogDescriptors.catalogoCanonico();
    }

    public NationalCommunicationInstitutionalSlaPredictiveDashboardResponse slaPreditivo(String uf, DestinatarioInstitucionalKind destinatarioKind) {
        return workflowOperations.slaPreditivo(uf, destinatarioKind);
    }

    public NationalCommunicationInstitutionalBulkResponse receberLote(NationalCommunicationInstitutionalBulkRequest request) {
        return workflowOperations.receberLote(request);
    }

    public NationalCommunicationInstitutionalBulkResponse certificarCienciaLote(NationalCommunicationInstitutionalBulkRequest request) {
        return workflowOperations.certificarCienciaLote(request);
    }

    public NationalCommunicationInstitutionalTriageSuggestionDashboardResponse triagemSugerida(String expedicaoUuid) {
        return workflowOperations.triagemSugerida(expedicaoUuid);
    }

    public List<NationalCommunicationInstitutionalIntegrationContractDescriptorResponse> contratoIntegracao() {
        return catalogDescriptors.contratoIntegracao();
    }

    public List<NationalCommunicationInstitutionalCoverageResponse> coberturasOperacionais(String unidadeCodigo) {
        return workflowOperations.coberturasOperacionais(unidadeCodigo);
    }

    public List<NationalCommunicationInstitutionalDelegationResponse> aplicarCoberturasAtivas(NationalCommunicationInstitutionalCoverageApplyRequest request) {
        return workflowOperations.aplicarCoberturasAtivas(request);
    }

    public List<NationalCommunicationInstitutionalPanelSummaryResponse> painelOrgao(String unidadeCodigo) {
        return workflowOperations.painelOrgao(unidadeCodigo);
    }

    public List<NationalCommunicationInstitutionalUnitQueueResponse> filasUnidade(String unidadeCodigo) {
        return workflowOperations.filasUnidade(unidadeCodigo);
    }

    public List<NationalCommunicationInstitutionalNoticeChannelResponse> avisosExternos() {
        return catalogDescriptors.avisosExternos();
    }

    public List<NationalCommunicationInstitutionalInboxItemResponse> pendentesNaoLeitura(String unidadeCodigo) {
        return workflowOperations.pendentesNaoLeitura(unidadeCodigo);
    }

    public NationalCommunicationInstitutionalActionResponse certificarNaoLeitura(NationalCommunicationInstitutionalNoReadRequest request) {
        return workflowOperations.certificarNaoLeitura(request);
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
        return entryLifecycle.contextosEntrada();
    }

    public List<NationalCommunicationInstitutionalOperationalLifecycleResponse> cadastrosOperacionais() {
        return entryLifecycle.cadastrosOperacionais();
    }

    public Optional<NationalCommunicationInstitutionalOperationalLifecycleResponse> detalharAfiliacaoLifecycle(String affiliationId) {
        return entryLifecycle.detalharAfiliacaoLifecycle(affiliationId);
    }

    public Optional<NationalCommunicationInstitutionalOperationalLifecycleResponse> detalharSolicitacaoLifecycle(String requestId) {
        return entryLifecycle.detalharSolicitacaoLifecycle(requestId);
    }

    public NationalCommunicationInstitutionalEntryGuardResponse guardiaoEntrada() {
        return entryLifecycle.guardiaoEntrada();
    }

    public NationalCommunicationInstitutionalFourLevelAccessResponse quatroNiveis(String affiliationId) {
        return entryLifecycle.quatroNiveis(affiliationId);
    }

    public List<NationalCommunicationInstitutionalOperationalCaseResponse> casosOperacionais(String affiliationId) {
        return entryLifecycle.casosOperacionais(affiliationId);
    }

    public NationalCommunicationInstitutionalStructuralDiagnosticResponse diagnosticoEstrutural(String affiliationId) {
        return entryLifecycle.diagnosticoEstrutural(affiliationId);
    }

    public List<NationalCommunicationInstitutionalTopologyResponse> topologiaDestinatarios() {
        return processWorkspace.topologiaDestinatarios();
    }

    public List<NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse> listarWorkspaces(Long processoId, String rito, String fase, String status, String ramo) {
        return processWorkspace.listarWorkspaces(processoId, rito, fase, status, ramo);
    }

    public NationalCommunicationInstitutionalProcessWorkspaceResponse detalharWorkspace(String profileCode, Long processoId, String rito, String fase, String status, String ramo) {
        return processWorkspace.detalharWorkspace(profileCode, processoId, rito, fase, status, ramo);
    }

    public NationalCommunicationInstitutionalProcessDiagnosticReportResponse diagnosticarWorkspace(Long processoId, String rito, String fase, String status, String ramo) {
        return processWorkspace.diagnosticarWorkspace(processoId, rito, fase, status, ramo);
    }

    public InstitutionalProceduralCoherenceReportResponse diagnosticarCoerencia(Long processoId, String rito, String fase, String status, String ramo) {
        return processWorkspace.diagnosticarCoerencia(processoId, rito, fase, status, ramo);
    }

    public NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse detalharCoerencia(String profileCode, Long processoId, String rito, String fase, String status, String ramo) {
        return processWorkspace.detalharCoerencia(profileCode, processoId, rito, fase, status, ramo);
    }

    public NationalCommunicationInstitutionalProceduralActEvaluationResponse avaliarAtoCoerencia(String profileCode, String actionCode, Long processoId, String rito, String fase, String status, String ramo) {
        return processWorkspace.avaliarAtoCoerencia(profileCode, actionCode, processoId, rito, fase, status, ramo);
    }

    public NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse fechamentoDelegado(String scope) {
        return affiliationGovernance.fechamentoDelegado(scope);
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
        return affiliationGovernance.entradaAtualDelegada();
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
        return affiliationGovernance.homologarAdesaoDelegada(requestId, request);
    }

    public List<NationalCommunicationInstitutionalAffiliationRequestResponse> listarAdesoesDelegadas() {
        return affiliationGovernance.listarAdesoesDelegadas();
    }

    public AdminInstitutionalPublicRecognitionResponse reconhecimentoPublicoAdesaoDelegada(String requestId) {
        return affiliationGovernance.reconhecimentoPublicoAdesaoDelegada(requestId);
    }

    public NationalCommunicationInstitutionalOfficialSourceDossierResponse dossieFontesOficiaisAdesaoDelegada(String requestId) {
        return affiliationGovernance.dossieFontesOficiaisAdesaoDelegada(requestId);
    }

    public NationalCommunicationInstitutionalOfficialIdentifierDossierResponse identificadoresOficiaisAdesaoDelegada(String requestId) {
        return affiliationGovernance.identificadoresOficiaisAdesaoDelegada(requestId);
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse atestacaoFontesOficiaisAdesaoDelegada(String requestId) {
        return affiliationGovernance.atestacaoFontesOficiaisAdesaoDelegada(requestId);
    }

    public NationalCommunicationInstitutionalOfficialSourceAttestationResponse revalidarFontesOficiaisAdesaoDelegada(String requestId,
                                                                                                                       NationalCommunicationInstitutionalOfficialSourceRevalidationRequest request) {
        return affiliationGovernance.revalidarFontesOficiaisAdesaoDelegada(requestId, request);
    }

    public List<NationalCommunicationInstitutionalTrustMatrixEntryResponse> matrizConfiabilidade(String scope) {
        return affiliationGovernance.matrizConfiabilidade(scope);
    }

    public List<NationalCommunicationInstitutionalPanelBlueprintResponse> painelBlueprints(String scope, String panel) {
        return catalogDescriptors.painelBlueprints(scope, panel);
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