package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalDelegatedAffiliationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.application.InstitutionalOperatingModelClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryActivationDecisionApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse;
import com.tcc.pjb.backend.model.dto.processual.NationalCommunicationInstitutionalTopologyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalFourLevelAccessResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalIdentityGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalRepresentativeVerificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationRequestResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalCanonicalCatalogEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalTextClosureAuditResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBindingApprovalResponse;
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
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.InstitutionalProceduralCoherenceReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralActEvaluationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessDiagnosticReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationContractDescriptorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialIdentifierDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceAttestationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceDossierResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalOfficialSourceRevalidationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalNoticeChannelResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalStructuralDiagnosticResponse;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import com.tcc.pjb.backend.service.security.context.PjbAuthenticatedSessionFacadeService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Cobertura completa dos 44 delegates de 1 linha que o facade principal expõe após a
 * extração das 5 sub-fachadas (Tasks 1-5). Cada teste prova UM delegate: que o argumento
 * chega intacto no objeto certo e que o valor devolvido pelo objeto certo é o que o
 * facade principal retorna, sem transformação silenciosa. Não cobre os 3 métodos-espinha
 * (entradaInteligente, modeloOperacional, solicitarAdesaoDelegada) -- eles têm lógica
 * própria no facade principal e não são delegates.
 */
class NationalCommunicationInstitutionalSurfaceFacadeServiceDelegationTest {

    private final NationalCommunicationInstitutionalAccessGuardSurfaceService accessGuard = mock(NationalCommunicationInstitutionalAccessGuardSurfaceService.class);
    private final NationalCommunicationInstitutionalCatalogDescriptorsSurfaceService catalogDescriptors = mock(NationalCommunicationInstitutionalCatalogDescriptorsSurfaceService.class);
    private final NationalCommunicationInstitutionalWorkflowOperationsSurfaceService workflowOperations = mock(NationalCommunicationInstitutionalWorkflowOperationsSurfaceService.class);
    private final NationalCommunicationInstitutionalEntryLifecycleSurfaceService entryLifecycle = mock(NationalCommunicationInstitutionalEntryLifecycleSurfaceService.class);
    private final NationalCommunicationInstitutionalProcessWorkspaceSurfaceService processWorkspace = mock(NationalCommunicationInstitutionalProcessWorkspaceSurfaceService.class);
    private final NationalCommunicationInstitutionalAffiliationGovernanceSurfaceService affiliationGovernance = mock(NationalCommunicationInstitutionalAffiliationGovernanceSurfaceService.class);
    private final InstitutionalEntryContextApplicationService entryContextApplicationService = mock(InstitutionalEntryContextApplicationService.class);
    private final InstitutionalEntryActivationDecisionApplicationService entryActivationDecisionApplicationService = mock(InstitutionalEntryActivationDecisionApplicationService.class);
    private final InstitutionalAffiliationApplicationService affiliationApplicationService = mock(InstitutionalAffiliationApplicationService.class);
    private final InstitutionalOperatingModelClosureApplicationService operatingModelClosureApplicationService = mock(InstitutionalOperatingModelClosureApplicationService.class);
    private final InstitutionalDelegatedAffiliationApplicationService delegatedAffiliationApplicationService = mock(InstitutionalDelegatedAffiliationApplicationService.class);
    private final NationalCommunicationInstitutionalFacadeSupport facadeSupport = mock(NationalCommunicationInstitutionalFacadeSupport.class);
    private final NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService = mock(NationalCommunicationInstitutionalStateBundleFacadeService.class);
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport = mock(NationalCommunicationInstitutionalSurfaceAssemblerSupport.class);
    private final PjbAuthenticatedSessionFacadeService authenticatedSessionFacadeService = mock(PjbAuthenticatedSessionFacadeService.class);

    private final NationalCommunicationInstitutionalSurfaceFacadeService service = new NationalCommunicationInstitutionalSurfaceFacadeService(
            accessGuard,
            catalogDescriptors,
            workflowOperations,
            entryContextApplicationService,
            entryActivationDecisionApplicationService,
            entryLifecycle,
            processWorkspace,
            affiliationGovernance,
            affiliationApplicationService,
            operatingModelClosureApplicationService,
            delegatedAffiliationApplicationService,
            facadeSupport,
            stateBundleFacadeService,
            surfaceAssemblerSupport,
            authenticatedSessionFacadeService
    );

    // ---------------- accessGuard (6 delegates) ----------------

    @Test
    void verificarRepresentanteDelegaParaAccessGuardComRequestIdIntacto() {
        var response = mock(NationalCommunicationInstitutionalRepresentativeVerificationResponse.class);
        when(accessGuard.verificarRepresentante("req-1")).thenReturn(Optional.of(response));

        assertThat(service.verificarRepresentante("req-1")).contains(response);
    }

    @Test
    void aprovacaoVinculoDelegaParaAccessGuardComAmbosOsIds() {
        var response = mock(NationalCommunicationInstitutionalBindingApprovalResponse.class);
        when(accessGuard.aprovacaoVinculo("aff-1", "nom-1")).thenReturn(response);

        assertThat(service.aprovacaoVinculo("aff-1", "nom-1")).isSameAs(response);
    }

    @Test
    void guardaIdentidadeDelegaParaAccessGuard() {
        var response = mock(NationalCommunicationInstitutionalIdentityGuardResponse.class);
        when(accessGuard.guardaIdentidade()).thenReturn(response);

        assertThat(service.guardaIdentidade()).isSameAs(response);
    }

    @Test
    void politicaStepUpDelegaParaAccessGuardComOs3Argumentos() {
        var response = mock(NationalCommunicationInstitutionalStepUpPolicyResponse.class);
        when(accessGuard.politicaStepUp("aff-1", "nom-1", "SENSITIVE")).thenReturn(response);

        assertThat(service.politicaStepUp("aff-1", "nom-1", "SENSITIVE")).isSameAs(response);
    }

    @Test
    void ativacaoContextoDelegaParaAccessGuardComOs5Argumentos() {
        var response = mock(NationalCommunicationInstitutionalContextActivationResponse.class);
        when(accessGuard.ativacaoContexto("aff-1", "nom-1", "UNI-1", "CX-1", "SENSITIVE")).thenReturn(response);

        assertThat(service.ativacaoContexto("aff-1", "nom-1", "UNI-1", "CX-1", "SENSITIVE")).isSameAs(response);
    }

    @Test
    void fechamentoTextoDelegaParaAccessGuard() {
        var response = mock(NationalCommunicationInstitutionalTextClosureAuditResponse.class);
        when(accessGuard.fechamentoTexto()).thenReturn(response);

        assertThat(service.fechamentoTexto()).isSameAs(response);
    }

    // ---------------- workflowOperations (14 delegates) ----------------

    @Test
    void catalogoCanonicoDelegaParaCatalogDescriptors() {
        var response = mock(NationalCommunicationInstitutionalCanonicalCatalogEntryResponse.class);
        when(catalogDescriptors.catalogoCanonico()).thenReturn(List.of(response));

        assertThat(service.catalogoCanonico()).containsExactly(response);
    }

    @Test
    void slaPreditivoDelegaParaWorkflowOperationsComUfEKind() {
        var response = mock(NationalCommunicationInstitutionalSlaPredictiveDashboardResponse.class);
        when(workflowOperations.slaPreditivo("CE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO)).thenReturn(response);

        assertThat(service.slaPreditivo("CE", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO)).isSameAs(response);
    }

    @Test
    void receberLoteDelegaParaWorkflowOperationsComRequestIntacto() {
        var request = new NationalCommunicationInstitutionalBulkRequest(List.of("uuid-1"), "detalhe");
        var response = mock(NationalCommunicationInstitutionalBulkResponse.class);
        when(workflowOperations.receberLote(request)).thenReturn(response);

        assertThat(service.receberLote(request)).isSameAs(response);
    }

    @Test
    void certificarCienciaLoteDelegaParaWorkflowOperationsComRequestIntacto() {
        var request = new NationalCommunicationInstitutionalBulkRequest(List.of("uuid-2"), "detalhe-2");
        var response = mock(NationalCommunicationInstitutionalBulkResponse.class);
        when(workflowOperations.certificarCienciaLote(request)).thenReturn(response);

        assertThat(service.certificarCienciaLote(request)).isSameAs(response);
    }

    @Test
    void triagemSugeridaDelegaParaWorkflowOperationsComUuid() {
        var response = mock(NationalCommunicationInstitutionalTriageSuggestionDashboardResponse.class);
        when(workflowOperations.triagemSugerida("exp-1")).thenReturn(response);

        assertThat(service.triagemSugerida("exp-1")).isSameAs(response);
    }

    @Test
    void contratoIntegracaoDelegaParaCatalogDescriptors() {
        var response = mock(NationalCommunicationInstitutionalIntegrationContractDescriptorResponse.class);
        when(catalogDescriptors.contratoIntegracao()).thenReturn(List.of(response));

        assertThat(service.contratoIntegracao()).containsExactly(response);
    }

    @Test
    void coberturasOperacionaisDelegaParaWorkflowOperationsComUnidade() {
        var response = mock(NationalCommunicationInstitutionalCoverageResponse.class);
        when(workflowOperations.coberturasOperacionais("UNI-1")).thenReturn(List.of(response));

        assertThat(service.coberturasOperacionais("UNI-1")).containsExactly(response);
    }

    @Test
    void aplicarCoberturasAtivasDelegaParaWorkflowOperationsComRequestIntacto() {
        var request = new NationalCommunicationInstitutionalCoverageApplyRequest("exp-3", "motivo");
        var response = mock(NationalCommunicationInstitutionalDelegationResponse.class);
        when(workflowOperations.aplicarCoberturasAtivas(request)).thenReturn(List.of(response));

        assertThat(service.aplicarCoberturasAtivas(request)).containsExactly(response);
    }

    @Test
    void painelOrgaoDelegaParaWorkflowOperationsComUnidade() {
        var response = mock(NationalCommunicationInstitutionalPanelSummaryResponse.class);
        when(workflowOperations.painelOrgao("UNI-2")).thenReturn(List.of(response));

        assertThat(service.painelOrgao("UNI-2")).containsExactly(response);
    }

    @Test
    void filasUnidadeDelegaParaWorkflowOperationsComUnidade() {
        var response = mock(NationalCommunicationInstitutionalUnitQueueResponse.class);
        when(workflowOperations.filasUnidade("UNI-3")).thenReturn(List.of(response));

        assertThat(service.filasUnidade("UNI-3")).containsExactly(response);
    }

    @Test
    void avisosExternosDelegaParaCatalogDescriptors() {
        var response = mock(NationalCommunicationInstitutionalNoticeChannelResponse.class);
        when(catalogDescriptors.avisosExternos()).thenReturn(List.of(response));

        assertThat(service.avisosExternos()).containsExactly(response);
    }

    @Test
    void pendentesNaoLeituraDelegaParaWorkflowOperationsComUnidade() {
        var response = mock(NationalCommunicationInstitutionalInboxItemResponse.class);
        when(workflowOperations.pendentesNaoLeitura("UNI-4")).thenReturn(List.of(response));

        assertThat(service.pendentesNaoLeitura("UNI-4")).containsExactly(response);
    }

    @Test
    void certificarNaoLeituraDelegaParaWorkflowOperationsComRequestIntacto() {
        var request = new NationalCommunicationInstitutionalNoReadRequest("exp-4", "motivo-4");
        var response = mock(NationalCommunicationInstitutionalActionResponse.class);
        when(workflowOperations.certificarNaoLeitura(request)).thenReturn(response);

        assertThat(service.certificarNaoLeitura(request)).isSameAs(response);
    }

    @Test
    void painelBlueprintsDelegaParaCatalogDescriptorsComScopeEPanel() {
        var response = mock(NationalCommunicationInstitutionalPanelBlueprintResponse.class);
        when(catalogDescriptors.painelBlueprints("BR", "PJB")).thenReturn(List.of(response));

        assertThat(service.painelBlueprints("BR", "PJB")).containsExactly(response);
    }

    // ---------------- entryLifecycle (8 delegates) ----------------

    @Test
    void contextosEntradaDelegaParaEntryLifecycle() {
        var response = mock(NationalCommunicationInstitutionalEntryContextResponse.class);
        when(entryLifecycle.contextosEntrada()).thenReturn(List.of(response));

        assertThat(service.contextosEntrada()).containsExactly(response);
    }

    @Test
    void cadastrosOperacionaisDelegaParaEntryLifecycle() {
        var response = mock(NationalCommunicationInstitutionalOperationalLifecycleResponse.class);
        when(entryLifecycle.cadastrosOperacionais()).thenReturn(List.of(response));

        assertThat(service.cadastrosOperacionais()).containsExactly(response);
    }

    @Test
    void detalharAfiliacaoLifecycleDelegaParaEntryLifecycleComAffiliationId() {
        var response = mock(NationalCommunicationInstitutionalOperationalLifecycleResponse.class);
        when(entryLifecycle.detalharAfiliacaoLifecycle("aff-2")).thenReturn(Optional.of(response));

        assertThat(service.detalharAfiliacaoLifecycle("aff-2")).contains(response);
    }

    @Test
    void detalharSolicitacaoLifecycleDelegaParaEntryLifecycleComRequestId() {
        var response = mock(NationalCommunicationInstitutionalOperationalLifecycleResponse.class);
        when(entryLifecycle.detalharSolicitacaoLifecycle("req-5")).thenReturn(Optional.of(response));

        assertThat(service.detalharSolicitacaoLifecycle("req-5")).contains(response);
    }

    @Test
    void guardiaoEntradaDelegaParaEntryLifecycle() {
        var response = mock(NationalCommunicationInstitutionalEntryGuardResponse.class);
        when(entryLifecycle.guardiaoEntrada()).thenReturn(response);

        assertThat(service.guardiaoEntrada()).isSameAs(response);
    }

    @Test
    void quatroNiveisDelegaParaEntryLifecycleComAffiliationId() {
        var response = mock(NationalCommunicationInstitutionalFourLevelAccessResponse.class);
        when(entryLifecycle.quatroNiveis("aff-3")).thenReturn(response);

        assertThat(service.quatroNiveis("aff-3")).isSameAs(response);
    }

    @Test
    void casosOperacionaisDelegaParaEntryLifecycleComAffiliationId() {
        var response = mock(NationalCommunicationInstitutionalOperationalCaseResponse.class);
        when(entryLifecycle.casosOperacionais("aff-4")).thenReturn(List.of(response));

        assertThat(service.casosOperacionais("aff-4")).containsExactly(response);
    }

    @Test
    void diagnosticoEstruturalDelegaParaEntryLifecycleComAffiliationId() {
        var response = mock(NationalCommunicationInstitutionalStructuralDiagnosticResponse.class);
        when(entryLifecycle.diagnosticoEstrutural("aff-5")).thenReturn(response);

        assertThat(service.diagnosticoEstrutural("aff-5")).isSameAs(response);
    }

    // ---------------- processWorkspace (7 delegates) ----------------

    @Test
    void topologiaDestinatariosDelegaParaProcessWorkspace() {
        var response = mock(NationalCommunicationInstitutionalTopologyResponse.class);
        when(processWorkspace.topologiaDestinatarios()).thenReturn(List.of(response));

        assertThat(service.topologiaDestinatarios()).containsExactly(response);
    }

    @Test
    void listarWorkspacesDelegaParaProcessWorkspaceComOs5Filtros() {
        var response = mock(NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse.class);
        when(processWorkspace.listarWorkspaces(10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(List.of(response));

        assertThat(service.listarWorkspaces(10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).containsExactly(response);
    }

    @Test
    void detalharWorkspaceDelegaParaProcessWorkspaceComOs6Filtros() {
        var response = mock(NationalCommunicationInstitutionalProcessWorkspaceResponse.class);
        when(processWorkspace.detalharWorkspace("perfil-1", 10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(response);

        assertThat(service.detalharWorkspace("perfil-1", 10L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }

    @Test
    void diagnosticarWorkspaceDelegaParaProcessWorkspaceComOs5Filtros() {
        var response = mock(NationalCommunicationInstitutionalProcessDiagnosticReportResponse.class);
        when(processWorkspace.diagnosticarWorkspace(11L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(response);

        assertThat(service.diagnosticarWorkspace(11L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }

    @Test
    void diagnosticarCoerenciaDelegaParaProcessWorkspaceComOs5Filtros() {
        var response = mock(InstitutionalProceduralCoherenceReportResponse.class);
        when(processWorkspace.diagnosticarCoerencia(12L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(response);

        assertThat(service.diagnosticarCoerencia(12L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }

    @Test
    void detalharCoerenciaDelegaParaProcessWorkspaceComOs6Filtros() {
        var response = mock(NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse.class);
        when(processWorkspace.detalharCoerencia("perfil-2", 13L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(response);

        assertThat(service.detalharCoerencia("perfil-2", 13L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }

    @Test
    void avaliarAtoCoerenciaDelegaParaProcessWorkspaceComOs7Filtros() {
        var response = mock(NationalCommunicationInstitutionalProceduralActEvaluationResponse.class);
        when(processWorkspace.avaliarAtoCoerencia("perfil-3", "PETICIONAR", 14L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).thenReturn(response);

        assertThat(service.avaliarAtoCoerencia("perfil-3", "PETICIONAR", 14L, "ORDINARIO", "CONHECIMENTO", "ATIVO", "CIVEL")).isSameAs(response);
    }

    // ---------------- affiliationGovernance (9 delegates) ----------------

    @Test
    void fechamentoDelegadoDelegaParaAffiliationGovernanceComScope() {
        var response = mock(NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse.class);
        when(affiliationGovernance.fechamentoDelegado("BR")).thenReturn(response);

        assertThat(service.fechamentoDelegado("BR")).isSameAs(response);
    }

    @Test
    void entradaAtualDelegadaDelegaParaAffiliationGovernance() {
        var response = mock(NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse.class);
        when(affiliationGovernance.entradaAtualDelegada()).thenReturn(response);

        assertThat(service.entradaAtualDelegada()).isSameAs(response);
    }

    @Test
    void homologarAdesaoDelegadaDelegaParaAffiliationGovernanceComRequestIntacto() {
        var request = new NationalCommunicationInstitutionalDelegatedAffiliationDecisionRequest(true, List.of("fundamento"));
        var response = mock(NationalCommunicationInstitutionalAffiliationRequestResponse.class);
        when(affiliationGovernance.homologarAdesaoDelegada("req-6", request)).thenReturn(response);

        assertThat(service.homologarAdesaoDelegada("req-6", request)).isSameAs(response);
    }

    @Test
    void listarAdesoesDelegadasDelegaParaAffiliationGovernance() {
        var response = mock(NationalCommunicationInstitutionalAffiliationRequestResponse.class);
        when(affiliationGovernance.listarAdesoesDelegadas()).thenReturn(List.of(response));

        assertThat(service.listarAdesoesDelegadas()).containsExactly(response);
    }

    @Test
    void reconhecimentoPublicoAdesaoDelegadaDelegaParaAffiliationGovernanceComRequestId() {
        var response = mock(AdminInstitutionalPublicRecognitionResponse.class);
        when(affiliationGovernance.reconhecimentoPublicoAdesaoDelegada("req-7")).thenReturn(response);

        assertThat(service.reconhecimentoPublicoAdesaoDelegada("req-7")).isSameAs(response);
    }

    @Test
    void dossieFontesOficiaisAdesaoDelegadaDelegaParaAffiliationGovernanceComRequestId() {
        var response = mock(NationalCommunicationInstitutionalOfficialSourceDossierResponse.class);
        when(affiliationGovernance.dossieFontesOficiaisAdesaoDelegada("req-8")).thenReturn(response);

        assertThat(service.dossieFontesOficiaisAdesaoDelegada("req-8")).isSameAs(response);
    }

    @Test
    void identificadoresOficiaisAdesaoDelegadaDelegaParaAffiliationGovernanceComRequestId() {
        var response = mock(NationalCommunicationInstitutionalOfficialIdentifierDossierResponse.class);
        when(affiliationGovernance.identificadoresOficiaisAdesaoDelegada("req-9")).thenReturn(response);

        assertThat(service.identificadoresOficiaisAdesaoDelegada("req-9")).isSameAs(response);
    }

    @Test
    void atestacaoFontesOficiaisAdesaoDelegadaDelegaParaAffiliationGovernanceComRequestId() {
        var response = mock(NationalCommunicationInstitutionalOfficialSourceAttestationResponse.class);
        when(affiliationGovernance.atestacaoFontesOficiaisAdesaoDelegada("req-10")).thenReturn(response);

        assertThat(service.atestacaoFontesOficiaisAdesaoDelegada("req-10")).isSameAs(response);
    }

    @Test
    void revalidarFontesOficiaisAdesaoDelegadaDelegaParaAffiliationGovernanceComRequestERevalidationRequest() {
        var request = mock(NationalCommunicationInstitutionalOfficialSourceRevalidationRequest.class);
        var response = mock(NationalCommunicationInstitutionalOfficialSourceAttestationResponse.class);
        when(affiliationGovernance.revalidarFontesOficiaisAdesaoDelegada("req-11", request)).thenReturn(response);

        assertThat(service.revalidarFontesOficiaisAdesaoDelegada("req-11", request)).isSameAs(response);
    }

    @Test
    void matrizConfiabilidadeDelegaParaAffiliationGovernanceComScope() {
        var response = mock(NationalCommunicationInstitutionalTrustMatrixEntryResponse.class);
        when(affiliationGovernance.matrizConfiabilidade("BR")).thenReturn(List.of(response));

        assertThat(service.matrizConfiabilidade("BR")).containsExactly(response);
    }

    // ---------------- invariante: os 4 colaboradores diretos que restaram NÃO são
    // usados pelos delegates -- só pelos 3 métodos-espinha (entradaInteligente,
    // modeloOperacional, solicitarAdesaoDelegada), fora do escopo deste teste. ----

    @Test
    void nenhumDelegateAcessaColaboradorApplicationServiceDireto() {
        service.verificarRepresentante("x");
        service.aprovacaoVinculo("x", "y");
        service.guardaIdentidade();
        service.politicaStepUp("x", "y", "z");
        service.ativacaoContexto("x", "y", "z", "w", "v");
        service.fechamentoTexto();
        service.catalogoCanonico();
        service.contextosEntrada();
        service.topologiaDestinatarios();
        service.fechamentoDelegado("x");
        service.matrizConfiabilidade("x");

        verifyNoInteractions(entryContextApplicationService, entryActivationDecisionApplicationService,
                affiliationApplicationService, operatingModelClosureApplicationService,
                delegatedAffiliationApplicationService, facadeSupport, stateBundleFacadeService,
                surfaceAssemblerSupport, authenticatedSessionFacadeService);
    }
}
