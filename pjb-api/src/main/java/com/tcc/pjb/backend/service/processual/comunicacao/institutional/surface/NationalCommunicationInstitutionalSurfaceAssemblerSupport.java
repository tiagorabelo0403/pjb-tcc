package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import com.tcc.pjb.backend.core.comunicacao.institucional.canonico.domain.InstitutionalCanonicalCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalIntegrationContractDescriptor;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelBlueprintSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain.InstitutionalRecipientTopologyEntry;
import com.tcc.pjb.backend.model.dto.processual.NationalCommunicationInstitutionalTopologyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalFourLevelAccessResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalIdentityGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalRepresentativeVerificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationDocumentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationRequestResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalDelegationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalCanonicalCatalogEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryActivationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntryGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalEntrySummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalTextClosureAuditResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalTextClosureItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBindingApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBootstrapAdministratorRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalDelegatedGovernanceItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalDelegatedScopeCoverageResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalProcessAuthorityBandResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalSlaPredictiveAlertResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalSlaPredictiveDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStepUpPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustMatrixEntryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalBulkResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalContextActivationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalCaseResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalLifecycleResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalInboxItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelBlueprintResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalPanelSummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalProcessQueueSectionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTriageSuggestionDashboardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalTriageSuggestionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalUnitQueueResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralActEvaluationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.InstitutionalProceduralCoherenceReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralCoherenceFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralCompetenceEnvelopeResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralContextVectorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralNextBestActResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessDiagnosticFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessDiagnosticReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessSeparatorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessVisualLaneResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIdentityBaseProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationContractDescriptorResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalNoticeChannelResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOperatingCoverageResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOperatingModelResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOperatingRoleBandResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalOperatingSeatResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalStructuralDiagnosticFindingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalStructuralDiagnosticResponse;
import com.tcc.pjb.backend.model.dto.security.context.PjbAuthenticatedSessionResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.access.InstitutionalRequestAccessContextFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.support.NationalCommunicationInstitutionalFacadeSupport;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.*;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.*;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.*;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.*;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.*;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.*;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.*;

@Service
public class NationalCommunicationInstitutionalSurfaceAssemblerSupport {

    private final InstitutionalRequestAccessContextFacadeService accessContextFacadeService;

    private final NationalCommunicationInstitutionalFacadeSupport facadeSupport;

    public NationalCommunicationInstitutionalSurfaceAssemblerSupport(NationalCommunicationInstitutionalFacadeSupport facadeSupport,
                                                                     InstitutionalRequestAccessContextFacadeService accessContextFacadeService) {
        this.facadeSupport = facadeSupport;
        this.accessContextFacadeService = accessContextFacadeService;
    }

    public NationalCommunicationInstitutionalCanonicalCatalogEntryResponse toCanonicalCatalogEntry(InstitutionalCanonicalCatalogEntry item) {
        return new NationalCommunicationInstitutionalCanonicalCatalogEntryResponse(
                item.atoCanonico().name(),
                item.destinatarioKind().name(),
                item.papelProcessual().name(),
                item.tipoComunicacao().name(),
                item.exigeCienciaPessoal(),
                item.bloqueiaMarcoProcessual(),
                item.gateCode(),
                item.canalPrincipalSugerido().name(),
                item.fallbacksSugeridos().stream().map(Enum::name).toList(),
                item.fundamentoLegal(),
                item.justificativas()
        );
    }

    public NationalCommunicationInstitutionalSlaPredictiveDashboardResponse toSlaDashboard(InstitutionalSlaPredictiveDashboard dashboard) {
        return new NationalCommunicationInstitutionalSlaPredictiveDashboardResponse(
                dashboard.alertas().stream().map(this::toSlaAlert).toList(),
                dashboard.totalUnidadesMonitoradas(),
                dashboard.totalCriticos(),
                dashboard.totalAltos(),
                dashboard.totalMedios(),
                dashboard.totalBaixos(),
                dashboard.generatedAt()
        );
    }

    public NationalCommunicationInstitutionalSlaPredictiveAlertResponse toSlaAlert(InstitutionalSlaPredictiveAlert item) {
        return new NationalCommunicationInstitutionalSlaPredictiveAlertResponse(
                item.unidadeCodigo(), item.unidadeSigla(), item.destinatarioKind().name(), item.uf(), item.pendenciasCiencia(), item.pendenciasCumprimento(), item.mediaHistoricaHorasResposta(), item.horasRestantesMinimas(), item.risco(), item.mensagem(), item.generatedAt()
        );
    }

    public NationalCommunicationInstitutionalBulkResponse toBulkResponse(InstitutionalBulkActionSummary result) {
        return new NationalCommunicationInstitutionalBulkResponse(result.operation(), result.totalRequested(), result.totalSucceeded(), result.totalFailed(), result.expedicoesSucesso(), result.failures(), result.processedAt());
    }

    public NationalCommunicationInstitutionalTriageSuggestionDashboardResponse toTriageDashboard(InstitutionalTriageSuggestionDashboard dashboard) {
        return new NationalCommunicationInstitutionalTriageSuggestionDashboardResponse(
                dashboard.expedicaoUuid(),
                dashboard.unidadeCodigo(),
                dashboard.caixaAtual(),
                dashboard.suggestions().stream().map(this::toTriageSuggestion).toList(),
                dashboard.notes(),
                dashboard.generatedAt()
        );
    }

    public NationalCommunicationInstitutionalTriageSuggestionResponse toTriageSuggestion(InstitutionalTriageSuggestion item) {
        return new NationalCommunicationInstitutionalTriageSuggestionResponse(
                item.suggestionId(), item.expedicaoUuid(), item.unidadeCodigo(), item.caixaCodigoOrigem(), item.caixaCodigoSugerida(), item.usuarioIdSugerido(), item.tipoSugestao(), item.score(), item.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalIntegrationContractDescriptorResponse toIntegrationContract(InstitutionalIntegrationContractDescriptor item) {
        return new NationalCommunicationInstitutionalIntegrationContractDescriptorResponse(
                item.provider(), item.canal(), item.version(), item.signatureAlgorithm(), item.requiredFields(), item.optionalFields(), item.transportGuarantees(), item.idempotencyKeyField(), item.correlationField(), item.notes()
        );
    }

    public NationalCommunicationInstitutionalDelegationResponse toDelegation(InstitutionalDelegationAssignment item) {
        return new NationalCommunicationInstitutionalDelegationResponse(
                item.assignmentId(), item.expedicaoUuid(), item.processoId(), item.unidadeCodigo(), item.caixaCodigo(), item.deleganteUsuarioId(), item.delegadoUsuarioId(), item.tipoFluxo().name(), item.capacidades().stream().map(Enum::name).toList(), item.status().name(), item.motivo(), item.inicioVigencia(), item.fimVigencia(), item.updatedAt()
        );
    }

    public NationalCommunicationInstitutionalNoticeChannelResponse toNoticeChannel(InstitutionalNoticeChannelDescriptor item) {
        return new NationalCommunicationInstitutionalNoticeChannelResponse(
                item.canal(), item.principalJuridico(), item.avisoInformativo(), item.finalidade(), item.observacao()
        );
    }

    public NationalCommunicationInstitutionalPanelSummaryResponse toResponse(InstitutionalOrgPanelSummary item) {
        InstitutionalRequestAccessContextFacadeService.InstitutionalAccessDigest digest = accessContextFacadeService.digest();
        return new NationalCommunicationInstitutionalPanelSummaryResponse(item.unidadeCodigo(), item.unidadeSigla(), item.destinatarioKind(), item.totalExpedientes(), item.pendentesRecebimento(), item.pendentesCiencia(), item.pendentesCumprimento(), item.atrasados(), item.caixasVisiveis(), digest.horizontalDataPlaneKey(), digest.rlsScopeKey(), digest.coverageMode(), digest.readOnly(), item.generatedAt());
    }

    public NationalCommunicationInstitutionalUnitQueueResponse toResponse(InstitutionalUnitQueueSummary item) {
        InstitutionalRequestAccessContextFacadeService.InstitutionalAccessDigest digest = accessContextFacadeService.digest();
        return new NationalCommunicationInstitutionalUnitQueueResponse(item.unidadeCodigo(), item.unidadeSigla(), item.caixaCodigo(), item.total(), item.disponibilizadas(), item.recebidas(), item.cientificadas(), item.cumpridas(), item.atrasadas(), digest.horizontalDataPlaneKey(), digest.rlsScopeKey(), digest.coverageMode(), digest.readOnly(), item.prazoMaisProximo(), item.generatedAt());
    }

    public NationalCommunicationInstitutionalActionResponse toActionResponse(InstitutionalInboxItem item) {
        return new NationalCommunicationInstitutionalActionResponse(item.expedicaoUuid(), item.status().name(), item.unidadeCodigo(), item.caixaCodigoAtual(), item.updatedAt());
    }

    public NationalCommunicationInstitutionalTopologyResponse toTopology(InstitutionalRecipientTopologyEntry entry) {
        return new NationalCommunicationInstitutionalTopologyResponse(
                entry.destinatarioInstitucionalKind().name(),
                entry.organizacaoExtraJudicialKind().name(),
                entry.legadosCompativeis().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()),
                entry.instituicaoEssencialJustica(),
                entry.apoioTecnicoOuAuxiliar(),
                entry.admiteCanalNacionalPessoal()
        );
    }
    public NationalCommunicationInstitutionalCoverageResponse toCoverage(InstitutionalOperationalCoverageRule item) {
        return new NationalCommunicationInstitutionalCoverageResponse(
                item.ruleId(), item.unidadeCodigo(), item.caixaCodigo(), item.titularUsuarioId(), item.coberturaUsuarioId(), item.tipoCobertura().name(), item.capacidades().stream().map(Enum::name).toList(), item.status().name(), item.inicioVigencia(), item.fimVigencia(), item.motivo(), item.observacoes(), item.createdAt(), item.updatedAt()
        );
    }

    public NationalCommunicationInstitutionalInboxItemResponse toInbox(InstitutionalInboxItem item) {
        InstitutionalRequestAccessContextFacadeService.InstitutionalAccessDigest digest = accessContextFacadeService.digest();
        return new NationalCommunicationInstitutionalInboxItemResponse(
                item.inboxItemId(), item.expedicaoUuid(), item.processoId(), item.processoNumero(), item.unidadeCodigo(), item.unidadeSigla(),
                item.destinatarioKind().name(), item.papelProcessual().name(), item.tipoComunicacao().name(),
                item.caixaCodigoOrigem(), item.caixaCodigoAtual(), item.canalPrincipal(), item.status().name(), item.gateCode(),
                item.bloqueiaFluxo(), item.atribuidoUsuarioId(), item.ultimoOperadorUsuarioId(), digest.horizontalDataPlaneKey(), digest.rlsScopeKey(), digest.coverageMode(), digest.readOnly(), digest.requiresStepUp(), digest.requiresQualifiedCertificate(), item.disponibilizadaEm(),
                item.recebidaEm(), item.cientificadaEm(), item.cumpridaEm(), item.prazoCienciaEm(), item.prazoRespostaEm(), item.updatedAt(), item.justificativas()
        );
    }

    public NationalCommunicationInstitutionalEntrySummaryResponse toResponse(InstitutionalEntrySummary summary,
                                                                                InstitutionalOperationalProfileProjection operationalProfile,
                                                                                InstitutionalEntryActivationDecision activationDecision,
                                                                                PjbAuthenticatedSessionResponse authenticatedSession) {
        return new NationalCommunicationInstitutionalEntrySummaryResponse(
                summary.usuarioId(),
                summary.nomeUsuario(),
                summary.tipoUsuario() == null ? null : summary.tipoUsuario().name(),
                toIdentityBase(summary.identidadeBase()),
                summary.possuiAmbientePessoal(),
                summary.possuiAmbienteInstitucional(),
                summary.contextos().stream().map(this::toContext).toList(),
                summary.contextoPreferencial() == null ? null : toContext(summary.contextoPreferencial()),
                toOperationalProfile(operationalProfile),
                toActivation(activationDecision),
                authenticatedSession,
                summary.generatedAt()
        );
    }

    public NationalCommunicationInstitutionalOperationalProfileResponse toOperationalProfile(InstitutionalOperationalProfileProjection item) {
        return facadeSupport.toResponse(item);
    }

    public NationalCommunicationInstitutionalEntryActivationResponse toActivation(InstitutionalEntryActivationDecision item) {
        return facadeSupport.toResponse(item);
    }

    public NationalCommunicationInstitutionalIdentityBaseProfileResponse toIdentityBase(InstitutionalIdentityBaseProfile item) {
        return facadeSupport.toIdentityBase(item);
    }

    public NationalCommunicationInstitutionalEntryContextResponse toContext(InstitutionalEntryContext item) {
        return facadeSupport.toContext(item);
    }

    public NationalCommunicationInstitutionalOperationalLifecycleResponse toLifecycle(InstitutionalOperationalLifecycle item) {
        return new NationalCommunicationInstitutionalOperationalLifecycleResponse(
                item.affiliationId(),
                item.requestId(),
                item.destinatarioKind() == null ? null : item.destinatarioKind().name(),
                item.organizationScope() == null ? null : item.organizationScope().name(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.uf(),
                item.comarca(),
                item.cnpj(),
                item.esferaAdministrativa(),
                item.ramosMateriais(),
                item.abrangenciasTerritoriais(),
                item.dominioInstitucional(),
                item.autoridadeAderenteCargo(),
                item.lifecycleStage().name(),
                item.afiliacaoHomologada(),
                item.possuiNomeacoesAtivas(),
                item.prontoParaAtivacao(),
                item.totalNomeacoes(),
                item.totalNomeacoesAtivas(),
                item.totalCaixas(),
                item.totalCaixasAtivas(),
                item.totalAdministradores(),
                item.caixasOperacionais(),
                item.canaisHabilitados(),
                item.politicaCiencia(),
                item.sla(),
                item.regrasFallback(),
                item.conveniosIntegracoes(),
                item.trilhosAutenticacao(),
                item.eixosAutorizacao(),
                item.fundamentos(),
                item.updatedAt()
        );
    }

    public NationalCommunicationInstitutionalEntryGuardResponse toGuard(InstitutionalEntryGuardSummary item) {
        return new NationalCommunicationInstitutionalEntryGuardResponse(
                item.userId(),
                item.userName(),
                toIdentityBase(item.identityBaseProfile()),
                item.identidadePessoalAutenticada(),
                item.vinculoInstitucionalValido(),
                item.contextoOperacionalAtivo(),
                item.autorizado(),
                item.affiliationId(),
                item.nominationId(),
                toAssessment(item.trustAssessment()),
                item.contextosAtivos().stream().map(this::toContext).toList(),
                item.trilhosAutenticacao(),
                item.eixosAutorizacao(),
                item.fundamentos(),
                item.evaluatedAt()
        );
    }

    public NationalCommunicationInstitutionalTrustAssessmentResponse toAssessment(com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment item) {
        return facadeSupport.toAssessment(item);
    }

    public NationalCommunicationInstitutionalFourLevelAccessResponse toResponse(InstitutionalFourLevelAccessSummary item) {
        return new NationalCommunicationInstitutionalFourLevelAccessResponse(
                item.userId(),
                item.userName(),
                item.tipoUsuario() == null ? null : item.tipoUsuario().name(),
                item.identityBaseCode(),
                item.affiliationId(),
                item.destinatarioKind() == null ? null : item.destinatarioKind().name(),
                item.organizationScope() == null ? null : item.organizationScope().name(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.caixaCodigo(),
                item.caixaNome(),
                item.nominationRole() == null ? null : item.nominationRole().name(),
                item.funcaoOperacional() == null ? null : item.funcaoOperacional().name(),
                item.processProfile() == null ? null : item.processProfile().name(),
                item.capacidades() == null ? java.util.Set.of() : item.capacidades().stream().map(Enum::name).collect(Collectors.toCollection(java.util.LinkedHashSet::new)),
                item.landingPanel() == null ? null : item.landingPanel().name(),
                item.cadastroInstitucionalResolvido(),
                item.estruturaInternaResolvida(),
                item.pessoaVinculada(),
                item.contextoOperacionalAtivo(),
                item.plantaoAtivo(),
                item.substituicaoAtiva(),
                item.delegacaoAtiva(),
                item.autorizado(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalOperationalCaseResponse toResponse(InstitutionalOperationalCaseSummary item) {
        return new NationalCommunicationInstitutionalOperationalCaseResponse(
                item.scenarioCode(),
                item.scenarioName(),
                item.destinatarioKind() == null ? null : item.destinatarioKind().name(),
                item.orgaoSigla(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.caixaCodigo(),
                item.caixaNome(),
                item.recebimentoPermitido(),
                item.triagemPermitida(),
                item.minutaPermitida(),
                item.assinaturaOuManifestacaoPermitida(),
                item.peticionamentoPermitido(),
                item.confirmacaoCustodiaPermitida(),
                item.registroCumprimentoPermitido(),
                item.titularObrigatorio(),
                item.landingPanel() == null ? null : item.landingPanel().name(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalStructuralDiagnosticResponse toResponse(InstitutionalStructuralDiagnosticReport item) {
        return new NationalCommunicationInstitutionalStructuralDiagnosticResponse(
                item.affiliationId(),
                item.compliant(),
                item.totalFindings(),
                item.blockingFindings(),
                item.findings().stream().map(this::toResponse).toList(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalStructuralDiagnosticFindingResponse toResponse(InstitutionalStructuralDiagnosticFinding item) {
        return new NationalCommunicationInstitutionalStructuralDiagnosticFindingResponse(
                item.code(),
                item.severity().name(),
                item.blocking(),
                item.targetType(),
                item.targetId(),
                item.message(),
                item.evidences());
    }

    public NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse toSummary(InstitutionalProcessWorkspaceSummary summary) {
        return new NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse(
                summary.profileCode(),
                summary.displayName(),
                summary.panel(),
                summary.processProfile(),
                summary.trustFloor(),
                summary.accentColor(),
                summary.totalActions(),
                summary.totalSections(),
                summary.totalAuthorityBands(),
                summary.totalSeparators(),
                summary.tabs(),
                summary.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProcessWorkspaceResponse toResponse(InstitutionalProcessWorkspace workspace) {
        return new NationalCommunicationInstitutionalProcessWorkspaceResponse(
                workspace.profileCode(),
                workspace.displayName(),
                workspace.panel(),
                workspace.processProfile(),
                workspace.trustFloor(),
                workspace.accentColor(),
                workspace.ritoProcessual(),
                workspace.faseProcessual(),
                workspace.statusProcessual(),
                workspace.ramoDireito(),
                workspace.tabs(),
                workspace.quickFilters(),
                workspace.recursosHabilitados(),
                workspace.embargosHabilitados(),
                workspace.actions().stream().map(this::toAction).toList(),
                workspace.sections().stream().map(this::toSection).toList(),
                workspace.visualLanes().stream().map(this::toVisualLane).toList(),
                workspace.authorityBands().stream().map(this::toAuthorityBand).toList(),
                workspace.separators().stream().map(this::toSeparator).toList(),
                workspace.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProcessActionResponse toAction(InstitutionalProcessActionSpec action) {
        return new NationalCommunicationInstitutionalProcessActionResponse(
                action.code(),
                action.title(),
                action.description(),
                action.accentColor(),
                action.requiresCertificate(),
                action.requiresTitularApproval(),
                action.modifiesFlow(),
                action.fasesPreferenciais(),
                action.ritosPreferenciais(),
                action.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProcessQueueSectionResponse toSection(InstitutionalProcessQueueSectionSpec section) {
        return new NationalCommunicationInstitutionalProcessQueueSectionResponse(
                section.code(),
                section.title(),
                section.accentColor(),
                section.ordem(),
                section.filtros(),
                section.indicadores(),
                section.ordenacoes()
        );
    }

    public NationalCommunicationInstitutionalProcessVisualLaneResponse toVisualLane(InstitutionalProcessVisualLaneSpec lane) {
        return new NationalCommunicationInstitutionalProcessVisualLaneResponse(
                lane.code(),
                lane.title(),
                lane.accentColor(),
                lane.ordem(),
                lane.active(),
                lane.filtros(),
                lane.etiquetas(),
                lane.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProcessAuthorityBandResponse toAuthorityBand(InstitutionalProcessAuthorityBand band) {
        return new NationalCommunicationInstitutionalProcessAuthorityBandResponse(
                band.code(),
                band.title(),
                band.accentColor(),
                band.enabled(),
                band.sensitive(),
                band.allowedActions(),
                band.prohibitedActions(),
                band.requiredGuards(),
                band.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProcessSeparatorResponse toSeparator(InstitutionalProcessSeparatorSpec separator) {
        return new NationalCommunicationInstitutionalProcessSeparatorResponse(
                separator.code(),
                separator.title(),
                separator.accentColor(),
                separator.ordem(),
                separator.active(),
                separator.filtros(),
                separator.marcadores(),
                separator.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProcessDiagnosticReportResponse toDiagnostic(InstitutionalProcessDiagnosticReport report) {
        return new NationalCommunicationInstitutionalProcessDiagnosticReportResponse(
                report.compliant(),
                report.totalFindings(),
                report.blockingFindings(),
                report.findings().stream().map(this::toDiagnosticFinding).toList(),
                report.fundamentos(),
                report.generatedAt()
        );
    }

    public NationalCommunicationInstitutionalProcessDiagnosticFindingResponse toDiagnosticFinding(InstitutionalProcessDiagnosticFinding finding) {
        return new NationalCommunicationInstitutionalProcessDiagnosticFindingResponse(
                finding.code(),
                finding.severity().name(),
                finding.blocking(),
                finding.profileCode(),
                finding.message(),
                finding.evidences()
        );
    }

    public NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse toAggregate(InstitutionalProceduralCoherenceAggregate aggregate) {
        return new NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse(
                toContext(aggregate.context()),
                toCompetence(aggregate.competenceEnvelope()),
                aggregate.aggregateFindings().stream().map(this::toFinding).toList(),
                aggregate.actEvaluations().stream().map(this::toActEvaluation).toList(),
                aggregate.nextBestActs().stream().map(this::toNextBestAct).toList(),
                aggregate.fundamentos(),
                aggregate.generatedAt()
        );
    }

    public InstitutionalProceduralCoherenceReportResponse toDiagnostic(InstitutionalProceduralCoherenceDiagnosticReport report) {
        return new InstitutionalProceduralCoherenceReportResponse(
                report.compliant(),
                report.totalFindings(),
                report.blockingFindings(),
                report.findings().stream().map(this::toFinding).toList(),
                report.fundamentos(),
                report.generatedAt()
        );
    }

    public NationalCommunicationInstitutionalProceduralContextVectorResponse toContext(InstitutionalProceduralContextVector context) {
        return new NationalCommunicationInstitutionalProceduralContextVectorResponse(
                context.profileCode(),
                context.displayName(),
                context.panel(),
                context.processProfile(),
                context.trustFloor(),
                context.ritoProcessual(),
                context.faseProcessual(),
                context.statusProcessual(),
                context.ramoDireito(),
                context.recursal(),
                context.embargos(),
                context.execucao(),
                context.urgente(),
                context.custodial(),
                context.technical(),
                context.governance(),
                context.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProceduralCompetenceEnvelopeResponse toCompetence(InstitutionalProceduralCompetenceEnvelope envelope) {
        return new NationalCommunicationInstitutionalProceduralCompetenceEnvelopeResponse(
                envelope.eixoMaterial(),
                envelope.eixoProcedimental(),
                envelope.eixoFasico(),
                envelope.eixoAtuacao(),
                envelope.exigeAssinaturaForte(),
                envelope.exigeSegregacaoTitular(),
                envelope.bloqueiaAtosPosArquivamento(),
                envelope.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProceduralActEvaluationResponse toActEvaluation(InstitutionalProceduralActEvaluation evaluation) {
        return new NationalCommunicationInstitutionalProceduralActEvaluationResponse(
                evaluation.actionCode(),
                evaluation.actionTitle(),
                evaluation.allowed(),
                evaluation.blocking(),
                evaluation.coherenceScore(),
                evaluation.decision(),
                evaluation.mandatoryGuards(),
                evaluation.findings().stream().map(this::toFinding).toList(),
                evaluation.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProceduralNextBestActResponse toNextBestAct(InstitutionalProceduralNextBestAct nextBestAct) {
        return new NationalCommunicationInstitutionalProceduralNextBestActResponse(
                nextBestAct.actionCode(),
                nextBestAct.actionTitle(),
                nextBestAct.priorityScore(),
                nextBestAct.rationale(),
                nextBestAct.expectedGuards(),
                nextBestAct.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalProceduralCoherenceFindingResponse toFinding(InstitutionalProceduralCoherenceFinding finding) {
        return new NationalCommunicationInstitutionalProceduralCoherenceFindingResponse(
                finding.code(),
                finding.severity().name(),
                finding.blocking(),
                finding.message(),
                finding.evidences(),
                finding.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedGovernanceClosure item) {
        return new NationalCommunicationInstitutionalDelegatedGovernanceClosureResponse(
                item.scopeFilter(),
                item.perfisDiretosPermitidos(),
                item.escoposDelegados().stream().map(this::toResponse).toList(),
                item.itens().stream().map(this::toResponse).toList(),
                item.fundamentos(),
                item.generatedAt()
        );
    }

    public NationalCommunicationInstitutionalDelegatedScopeCoverageResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedScopeCoverage item) {
        return new NationalCommunicationInstitutionalDelegatedScopeCoverageResponse(
                item.organizationScope(),
                item.displayName(),
                item.delegatedInstitutionalEntry(),
                item.forumOrJudicialUnit(),
                item.lanes(),
                item.guardRails(),
                item.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalDelegatedGovernanceItemResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedGovernanceItem item) {
        return new NationalCommunicationInstitutionalDelegatedGovernanceItemResponse(
                item.closureId(),
                item.affiliationId(),
                item.requestId(),
                item.organizationScope(),
                item.destinatarioKind(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.forumOrJudicialUnit(),
                item.delegatedInstitutionalEntry(),
                item.adesaoAptaParaHomologacao(),
                item.duplaChaveSatisfeita(),
                item.afiliacaoHomologada(),
                item.nomeacoesAtivas(),
                item.quatroNiveisFechados(),
                item.orgaoNomeiaEPjbHomologa(),
                item.recertificacaoEmDia(),
                item.diagnosticoEstruturalOk(),
                item.integracaoEndurecida(),
                item.totalNomeacoes(),
                item.totalNomeacoesAtivas(),
                item.totalAdministradores(),
                item.totalCaixasAtivas(),
                item.caixasOperacionais(),
                item.guardRails(),
                item.missingPillars(),
                item.fundamentos(),
                item.updatedAt()
        );
    }

    public NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedCurrentEntryClosure item) {
        return new NationalCommunicationInstitutionalDelegatedCurrentEntryClosureResponse(
                item.userId(),
                item.identityCode(),
                item.possuiAmbientePessoal(),
                item.possuiAmbienteInstitucional(),
                item.possuiPerfilDiretoAutorizado(),
                item.possuiContextoDelegadoAtivo(),
                item.perfisDiretosPermitidos(),
                item.contextosDelegados(),
                item.fundamentos(),
                item.generatedAt()
        );
    }

    public NationalCommunicationInstitutionalAffiliationRequestResponse toResponse(InstitutionalAffiliationRequest item) {
        return new NationalCommunicationInstitutionalAffiliationRequestResponse(
                item.requestId(),
                item.destinatarioKind().name(),
                item.organizationScope() == null ? null : item.organizationScope().name(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.unidadeCodigo(),
                item.unidadeNome(),
                item.uf(),
                item.comarca(),
                item.cnpj(),
                item.esferaAdministrativa(),
                item.ramosMateriais(),
                item.abrangenciasTerritoriais(),
                item.dominioInstitucional(),
                item.autoridadeAderenteCargo(),
                item.representanteUsuarioId(),
                item.representanteNome(),
                item.representativeRole() == null ? null : item.representativeRole().name(),
                item.bootstrapAdministrators().entrySet().stream().map(entry -> new NationalCommunicationInstitutionalBootstrapAdministratorRequest(entry.getKey(), entry.getValue())).toList(),
                item.trustFloorProposto() == null ? null : item.trustFloorProposto().name(),
                item.requerDuplaAprovacaoAdministrador(),
                item.requerCertificadoICP(),
                item.restringeCertificadoRedeInstitucional(),
                item.permiteUsoRemotoComAutorizacao(),
                item.canaisHabilitados(),
                item.politicaCiencia(),
                item.sla(),
                item.regrasFallback(),
                item.conveniosIntegracoes(),
                item.documentos().stream().map(this::toResponse).toList(),
                item.status().name(),
                item.materializedAffiliationId(),
                item.fundamentos(),
                item.createdAt(),
                item.decidedAt(),
                item.updatedAt()
        );
    }

    public NationalCommunicationInstitutionalAffiliationDocumentResponse toResponse(InstitutionalAffiliationDocument item) {
        return new NationalCommunicationInstitutionalAffiliationDocumentResponse(
                item.codigo(),
                item.nome(),
                item.tipo(),
                item.referenciaExterna(),
                item.hashDocumento(),
                item.obrigatorio(),
                item.validado()
        );
    }

    public NationalCommunicationInstitutionalTrustMatrixEntryResponse toResponse(InstitutionalTrustMatrixEntry item) {
        return new NationalCommunicationInstitutionalTrustMatrixEntryResponse(
                item.codigo(),
                item.escopo(),
                item.nomeExibicao(),
                item.entryMode(),
                item.laneKind(),
                item.nominationRole(),
                item.processProfile(),
                item.panel(),
                item.trustFloor(),
                item.fatoresObrigatorios(),
                item.fatoresComplementares(),
                item.capacidadesPermitidas(),
                item.restricoes(),
                item.guardRails(),
                item.rotasIniciais(),
                item.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalPanelBlueprintResponse toResponse(InstitutionalPanelBlueprintSpec item) {
        return new NationalCommunicationInstitutionalPanelBlueprintResponse(
                item.codigo(),
                item.escopo(),
                item.panel(),
                item.audience(),
                item.titulo(),
                item.rotaInicial(),
                item.secoesPrimarias(),
                item.acoesRapidas(),
                item.guardasSeguranca(),
                item.regrasVisibilidade(),
                item.fundamentos()
        );
    }

    public NationalCommunicationInstitutionalRepresentativeVerificationResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalRepresentativeVerification item) {
        return new NationalCommunicationInstitutionalRepresentativeVerificationResponse(
                item.requestId(),
                item.representativeUserId(),
                item.representativeName(),
                item.representativeRole(),
                item.authorityTitle(),
                item.representativeIdentityComplete(),
                item.representativeDocumentValidated(),
                item.institutionalDomainValidated(),
                item.certificateMaterialValidated(),
                item.trustChainValidated(),
                item.dualKeySatisfied(),
                item.homologationReady(),
                item.findings(),
                item.fundamentos(),
                item.checkedAt());
    }

    public NationalCommunicationInstitutionalBindingApprovalResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalBindingApproval item) {
        return new NationalCommunicationInstitutionalBindingApprovalResponse(
                item.userId(),
                item.userName(),
                item.affiliationId(),
                item.nominationId(),
                item.unidadeCodigo(),
                item.caixaCodigo(),
                item.affiliationActive(),
                item.nominationActive(),
                item.dualAdministrationSatisfied(),
                item.recertificationDue(),
                item.capacityBound(),
                item.homologated(),
                item.approved(),
                item.findings(),
                item.fundamentos(),
                item.checkedAt());
    }

    public NationalCommunicationInstitutionalIdentityGuardResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalIdentityGuardDecision item) {
        return new NationalCommunicationInstitutionalIdentityGuardResponse(
                item.userId(),
                item.userName(),
                item.identityCode(),
                item.tipoUsuarioBase(),
                item.directPersonalEntryAllowed(),
                item.institutionalAdhesionRequired(),
                item.preferredEntryMode(),
                item.preferredPanel(),
                item.trustFloor(),
                item.requiresInstitutionalNomination(),
                item.directProfiles(),
                item.institutionalProfiles(),
                item.fundamentos(),
                item.checkedAt());
    }

    public NationalCommunicationInstitutionalStepUpPolicyResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStepUpAuthenticationPolicy item) {
        return new NationalCommunicationInstitutionalStepUpPolicyResponse(
                item.userId(),
                item.userName(),
                item.affiliationId(),
                item.nominationId(),
                item.sensitiveAct(),
                item.requiresMfa(),
                item.requiresQualifiedCertificate(),
                item.requiresInstitutionalNetwork(),
                item.acceptsRemoteCertificateAuthorization(),
                item.requiresManualApproval(),
                item.blocked(),
                item.findings(),
                item.fundamentos(),
                item.checkedAt());
    }

    public NationalCommunicationInstitutionalContextActivationResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalContextActivationDecision item) {
        return new NationalCommunicationInstitutionalContextActivationResponse(
                item.userId(),
                item.userName(),
                item.identityCode(),
                item.affiliationId(),
                item.nominationId(),
                item.unidadeCodigo(),
                item.caixaCodigo(),
                item.personalIdentityAuthenticated(),
                item.institutionalBindingValid(),
                item.operationalContextActive(),
                item.requiresStepUp(),
                item.requiresManualApproval(),
                item.blocked(),
                item.allowed(),
                item.findings(),
                item.fundamentos(),
                item.checkedAt());
    }

    public NationalCommunicationInstitutionalTextClosureAuditResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalTextClosureAudit item) {
        return new NationalCommunicationInstitutionalTextClosureAuditResponse(
                item.auditId(),
                item.fullyClosed(),
                item.totalItems(),
                item.implementedItems(),
                item.items().stream().map(this::toResponse).toList(),
                item.fundamentos(),
                item.checkedAt());
    }

    public NationalCommunicationInstitutionalTextClosureItemResponse toResponse(com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalTextClosureItem item) {
        return new NationalCommunicationInstitutionalTextClosureItemResponse(item.code(), item.eixo(), item.implemented(), item.evidences(), item.fundamentos());
    }

    public NationalCommunicationInstitutionalOperatingModelResponse toResponse(InstitutionalOperatingModelClosure item) {
        return new NationalCommunicationInstitutionalOperatingModelResponse(
                item.affiliationId(),
                item.orgaoSigla(),
                item.orgaoNome(),
                item.destinatarioKind(),
                item.organizationScope(),
                item.blueprintCode(),
                item.entryMode(),
                item.institutionManagedRoles(),
                item.personalRootIdentityRequired(),
                item.magistratesEnterThroughForumAndPersonalAccess(),
                item.coverageMode(),
                toResponse(item.coverageRoute()),
                item.administrativeSeats().stream().map(this::toResponse).toList(),
                item.roleBands().stream().map(this::toResponse).toList(),
                item.findings(),
                item.fundamentos(),
                item.generatedAt());
    }

    public NationalCommunicationInstitutionalOperatingCoverageResponse toResponse(InstitutionalOperatingCoverageRoute item) {
        if (item == null) {
            return null;
        }
        return new NationalCommunicationInstitutionalOperatingCoverageResponse(
                item.requestedMunicipality(),
                item.requestedUf(),
                item.requestedKind(),
                item.localUnitPresent(),
                item.responsibleUnitCode(),
                item.responsibleUnitName(),
                item.responsibleForo(),
                item.responsibleComarca(),
                item.responsibleTribunalCode(),
                item.coverageMode(),
                item.fallbackChain(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalOperatingSeatResponse toResponse(InstitutionalOperatingAdministrativeSeat item) {
        return new NationalCommunicationInstitutionalOperatingSeatResponse(
                item.code(),
                item.displayName(),
                item.laneKind(),
                item.nominationRole(),
                item.processProfile(),
                item.trustFloor(),
                item.managementSeat(),
                item.requiresStepUp(),
                item.requiresCertificate(),
                item.remoteAuthorized(),
                item.capacities(),
                item.restrictions(),
                item.fundamentos());
    }

    public NationalCommunicationInstitutionalOperatingRoleBandResponse toResponse(InstitutionalOperatingRoleBand item) {
        return new NationalCommunicationInstitutionalOperatingRoleBandResponse(
                item.bandKey(),
                item.laneKind(),
                item.nominationRole(),
                item.tipoUsuario(),
                item.displayName(),
                item.activeNominations(),
                item.judicialAuthority(),
                item.institutionalOnly(),
                item.personalDirectEntryAllowed(),
                item.capacities(),
                item.fundamentos());
    }

}
