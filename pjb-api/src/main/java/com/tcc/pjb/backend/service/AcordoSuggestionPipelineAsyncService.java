package com.tcc.pjb.backend.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextService;
import com.tcc.pjb.backend.core.kernel.advisory.KernelAdvisoryTelemetry;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.kernel.governance.InstitutionalPolicyResolver;
import com.tcc.pjb.backend.core.kernel.governance.InstitutionalPolicySnapshotReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelDecisionMetricsReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelDecisionMetricsService;
import com.tcc.pjb.backend.core.kernel.governance.KernelRiskEscalationReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelRiskEscalationService;
import com.tcc.pjb.backend.core.kernel.governance.NegotiationMessageDecision;
import com.tcc.pjb.backend.core.kernel.governance.NegotiationReleaseGuard;
import com.tcc.pjb.backend.core.kernel.governance.ProcessIntelligenceSnapshotService;
import com.tcc.pjb.backend.model.dto.EssenceResult;
import com.tcc.pjb.backend.model.dto.IARunResult;
import com.tcc.pjb.backend.model.dto.IaSettings;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Profile;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import com.tcc.pjb.backend.model.repository.ChatMensagemRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AcordoSuggestionPipelineAsyncService {

    private static final Duration ACORDO_PIPELINE_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration ACORDO_PIPELINE_READ_BUDGET = Duration.ofSeconds(6);
    private static final Duration ACORDO_PIPELINE_WRITE_BUDGET = Duration.ofSeconds(8);

    private final PropostaAcordoRepository propostaAcordoRepository;
    private final UsuarioRepository usuarioRepository;
    private final IAOrchestrator iaOrchestrator;
    private final EssenceFilter essenceFilter;
    private final ProfileEngine profileEngine;
    private final AuditService auditService;
    private final ChatService chatService;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;
    private final SettlementAdvisoryService settlementAdvisoryService;
    private final InstitutionalGovernanceContextService institutionalGovernanceContextService;
    private final NegotiationMemoryService negotiationMemoryService;
    private final NegotiationExplainabilityService negotiationExplainabilityService;
    private final NegotiationChatDigestService negotiationChatDigestService;
    private final NegotiationApprovalMatrixService negotiationApprovalMatrixService;
    private final NegotiationChannelGovernanceService negotiationChannelGovernanceService;
    private final KernelOperationalGovernanceService kernelOperationalGovernanceService;
    private final InstitutionalPolicyResolver institutionalPolicyResolver;
    private final KernelDecisionMetricsService kernelDecisionMetricsService;
    private final KernelRiskEscalationService kernelRiskEscalationService;
    private final NegotiationReleaseGuard negotiationReleaseGuard;
    private final ProcessIntelligenceSnapshotService processIntelligenceSnapshotService;
    private final ChatMensagemRepository chatMensagemRepository;
    private final PjbTransactionalExecutionSupport transactionalExecutionSupport;

    public AcordoSuggestionPipelineAsyncService(PropostaAcordoRepository propostaAcordoRepository,
                                               UsuarioRepository usuarioRepository,
                                               IAOrchestrator iaOrchestrator,
                                               EssenceFilter essenceFilter,
                                               ProfileEngine profileEngine,
                                               AuditService auditService,
                                               ChatService chatService,
                                               ProcessoRitoSnapshotService processoRitoSnapshotService,
                                               SettlementAdvisoryService settlementAdvisoryService,
                                               InstitutionalGovernanceContextService institutionalGovernanceContextService,
                                               NegotiationMemoryService negotiationMemoryService,
                                               NegotiationExplainabilityService negotiationExplainabilityService,
                                               NegotiationChatDigestService negotiationChatDigestService,
                                               NegotiationApprovalMatrixService negotiationApprovalMatrixService,
                                               NegotiationChannelGovernanceService negotiationChannelGovernanceService,
                                               KernelOperationalGovernanceService kernelOperationalGovernanceService,
                                               InstitutionalPolicyResolver institutionalPolicyResolver,
                                               KernelDecisionMetricsService kernelDecisionMetricsService,
                                               KernelRiskEscalationService kernelRiskEscalationService,
                                               NegotiationReleaseGuard negotiationReleaseGuard,
                                               ProcessIntelligenceSnapshotService processIntelligenceSnapshotService,
                                               ChatMensagemRepository chatMensagemRepository,
                                               PjbTransactionalExecutionSupport transactionalExecutionSupport) {
        this.propostaAcordoRepository = Objects.requireNonNull(propostaAcordoRepository, "propostaAcordoRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.iaOrchestrator = Objects.requireNonNull(iaOrchestrator, "iaOrchestrator");
        this.essenceFilter = Objects.requireNonNull(essenceFilter, "essenceFilter");
        this.profileEngine = Objects.requireNonNull(profileEngine, "profileEngine");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.chatService = Objects.requireNonNull(chatService, "chatService");
        this.processoRitoSnapshotService = Objects.requireNonNull(processoRitoSnapshotService, "processoRitoSnapshotService");
        this.settlementAdvisoryService = Objects.requireNonNull(settlementAdvisoryService, "settlementAdvisoryService");
        this.institutionalGovernanceContextService = Objects.requireNonNull(institutionalGovernanceContextService, "institutionalGovernanceContextService");
        this.negotiationMemoryService = Objects.requireNonNull(negotiationMemoryService, "negotiationMemoryService");
        this.negotiationExplainabilityService = Objects.requireNonNull(negotiationExplainabilityService, "negotiationExplainabilityService");
        this.negotiationChatDigestService = Objects.requireNonNull(negotiationChatDigestService, "negotiationChatDigestService");
        this.negotiationApprovalMatrixService = Objects.requireNonNull(negotiationApprovalMatrixService, "negotiationApprovalMatrixService");
        this.negotiationChannelGovernanceService = Objects.requireNonNull(negotiationChannelGovernanceService, "negotiationChannelGovernanceService");
        this.kernelOperationalGovernanceService = Objects.requireNonNull(kernelOperationalGovernanceService, "kernelOperationalGovernanceService");
        this.institutionalPolicyResolver = Objects.requireNonNull(institutionalPolicyResolver, "institutionalPolicyResolver");
        this.kernelDecisionMetricsService = Objects.requireNonNull(kernelDecisionMetricsService, "kernelDecisionMetricsService");
        this.kernelRiskEscalationService = Objects.requireNonNull(kernelRiskEscalationService, "kernelRiskEscalationService");
        this.negotiationReleaseGuard = Objects.requireNonNull(negotiationReleaseGuard, "negotiationReleaseGuard");
        this.processIntelligenceSnapshotService = Objects.requireNonNull(processIntelligenceSnapshotService, "processIntelligenceSnapshotService");
        this.chatMensagemRepository = Objects.requireNonNull(chatMensagemRepository, "chatMensagemRepository");
        this.transactionalExecutionSupport = Objects.requireNonNull(transactionalExecutionSupport, "transactionalExecutionSupport");
    }

    public void runForProposal(Long propostaId) {
        if (propostaId == null) {
            return;
        }
        transactionalExecutionSupport.run(
                PjbExecutionDescriptor.job("acordo-suggestion-pipeline.run-for-proposal", ACORDO_PIPELINE_TIMEOUT),
                () -> runForProposalInternal(propostaId)
        );
    }

    private void runForProposalInternal(Long propostaId) {
        if (propostaId == null) {
            return;
        }
        AcordoPipelineSeed seed = transactionalExecutionSupport.executeReadOnly(
                "acordo-suggestion-pipeline.load-seed",
                ACORDO_PIPELINE_READ_BUDGET,
                () -> loadPipelineSeed(propostaId)
        );
        if (seed == null) {
            return;
        }

        Profile profile = profileEngine.loadProfile(
                seed.processo().getModulo() != null ? seed.processo().getModulo().name() : null,
                seed.processo().getJurisdicao(),
                mapCompetencia(seed.processo())
        );

        IARunResult run = gerarRunSafeDeAcordo(seed.processo(), seed.proposta(), seed.settings(), profile);
        EssenceResult decision = essenceFilter.evaluate(seed.proposta().getTermosHtml(), run.getHtml());
        boolean contemClausulasAbusivas = essenceFilter.detectAbusiveClauses(run.getHtml());
        AcordoPipelineOutcome outcome = buildOutcome(run, decision, contemClausulasAbusivas);
        AcordoPipelineAnalysis analysis = buildAnalysis(seed);

        transactionalExecutionSupport.executeInNewTransaction(
                "acordo-suggestion-pipeline.persist-outcome",
                ACORDO_PIPELINE_WRITE_BUDGET,
                () -> persistPipelineOutcome(seed, outcome, analysis)
        );
    }

    private AcordoPipelineSeed loadPipelineSeed(Long propostaId) {
        PropostaAcordo proposta = propostaAcordoRepository.findById(propostaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta", propostaId));
        IaSettings settings = proposta.getSettings();
        if (settings == null || !settings.isSuggestionsEnabled()) {
            return null;
        }
        Processo processo = proposta.getProcesso();
        if (processo == null) {
            return null;
        }
        warmDetachedReferences(processo, proposta, settings);
        String ritoName = processoRitoSnapshotService.resolve(processo, null).ritoCode();
        List<ChatMensagem> recentChat = recentChat(processo.getId());
        return new AcordoPipelineSeed(proposta.getId(), processo.getId(), proposta, processo, settings, ritoName, recentChat);
    }

    private AcordoPipelineOutcome buildOutcome(IARunResult run, EssenceResult decision, boolean contemClausulasAbusivas) {
        boolean revisaoHumana = !decision.isEssencePreserved() || contemClausulasAbusivas;
        String motivo = !decision.isEssencePreserved()
                ? "Alteração de essência detectada."
                : contemClausulasAbusivas ? "Cláusulas potencialmente abusivas identificadas." : null;
        StatusAcordo status = revisaoHumana ? StatusAcordo.AGUARDANDO_REVISAO_HUMANA : StatusAcordo.EM_NEGOCIACAO;
        String mensagem = revisaoHumana
                ? "Alerta: " + motivo + " Revisão obrigatória."
                : "Sugestões de acordo geradas e aplicadas.";
        return new AcordoPipelineOutcome(run, decision, contemClausulasAbusivas, revisaoHumana, status, motivo, mensagem);
    }

    private AcordoPipelineAnalysis buildAnalysis(AcordoPipelineSeed seed) {
        SettlementAdvisoryReport settlementAdvisory = settlementAdvisoryService.analyze(
                seed.processo(),
                seed.ritoName(),
                seed.proposta().getValorAcordo(),
                buildNegotiationSignals(seed.processo()),
                null
        );
        InstitutionalGovernanceContextReport governance = institutionalGovernanceContextService.analyzeProcess(seed.processo(), seed.ritoName(), settlementAdvisory, null, null);
        NegotiationMemoryReport negotiationMemory = negotiationMemoryService.analyzeProcess(seed.processo(), seed.proposta(), seed.recentChat(), settlementAdvisory, governance);
        NegotiationExplainabilityReport negotiationExplainability = negotiationExplainabilityService.compose(seed.processo(), seed.proposta(), seed.recentChat(), settlementAdvisory, negotiationMemory, governance);
        KernelOperationalGovernanceReport kernelOperationalGovernance = kernelOperationalGovernanceService.analyzeProcess(seed.processo(), seed.ritoName(), null, null, governance, negotiationMemory, negotiationExplainability, null, null);
        NegotiationChatDigestReport negotiationChatDigest = negotiationChatDigestService.analyzeProcess(seed.processo(), seed.proposta(), seed.recentChat(), settlementAdvisory, negotiationMemory, negotiationExplainability, governance, kernelOperationalGovernance);
        NegotiationApprovalMatrixReport negotiationApprovalMatrix = negotiationApprovalMatrixService.analyzeProcess(seed.processo(), seed.proposta(), seed.recentChat(), governance, kernelOperationalGovernance, negotiationMemory, negotiationExplainability, negotiationChatDigest);
        NegotiationChannelGovernanceReport negotiationChannelGovernance = negotiationChannelGovernanceService.analyzeProcess(seed.processo(), seed.proposta(), seed.recentChat(), governance, kernelOperationalGovernance, negotiationMemory, negotiationExplainability, negotiationChatDigest, negotiationApprovalMatrix);
        InstitutionalPolicySnapshotReport institutionalPolicySnapshot = institutionalPolicyResolver.resolve(seed.processo(), seed.proposta(), seed.recentChat(), governance, negotiationChatDigest, negotiationApprovalMatrix, negotiationChannelGovernance, seed.ritoName());
        KernelDecisionMetricsReport kernelDecisionMetrics = kernelDecisionMetricsService.analyzeProcess(seed.processo());
        KernelRiskEscalationReport kernelRiskEscalation = kernelRiskEscalationService.analyzeProcess(seed.processo(), institutionalPolicySnapshot, kernelDecisionMetrics, negotiationChatDigest, negotiationApprovalMatrix, negotiationChannelGovernance);
        NegotiationMessageDecision governedMessageDecision = negotiationReleaseGuard.decide(
                negotiationChatDigest != null ? negotiationChatDigest.suggestedNextMessage() : null,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance
        );
        KernelAdvisoryTelemetry telemetry = kernelOperationalGovernanceService.buildTelemetry(
                "ACORDO_PIPELINE",
                seed.ritoName(),
                settlementAdvisory,
                governance,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision
        );
        List<String> strategicFocus = buildStrategicFocus(
                settlementAdvisory,
                governance,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision
        );
        String intelligenceDigest = buildIntelligenceDigest(
                settlementAdvisory,
                governance,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision,
                telemetry
        );
        String suggestedNextMessage = negotiationChatDigest != null ? negotiationChatDigest.suggestedNextMessage() : null;
        return new AcordoPipelineAnalysis(
                settlementAdvisory,
                governance,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChatDigest,
                negotiationApprovalMatrix,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision,
                telemetry,
                strategicFocus,
                intelligenceDigest,
                suggestedNextMessage
        );
    }

    private void persistPipelineOutcome(AcordoPipelineSeed seed,
                                        AcordoPipelineOutcome outcome,
                                        AcordoPipelineAnalysis analysis) {
        PropostaAcordo proposta = propostaAcordoRepository.findById(seed.propostaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta", seed.propostaId()));
        Processo processo = proposta.getProcesso();
        if (processo == null) {
            throw new IllegalStateException("Processo não encontrado para proposta " + seed.propostaId());
        }
        Usuario usuarioSistema = buscarUsuarioSistema();
        auditService.recordIARun(proposta.getUuid(), outcome.run(), outcome.decision());
        proposta.setIarunId(outcome.run().getRunId());
        proposta.setStatus(outcome.status());
        if (outcome.revisaoHumana()) {
            chatService.postarMensagemSistema(processo, usuarioSistema, outcome.mensagemSistema());
        } else {
            proposta.setTermosHtml(outcome.run().getHtml());
            chatService.postarMensagemSistema(processo, usuarioSistema, outcome.mensagemSistema());
        }
        propostaAcordoRepository.save(proposta);
        processIntelligenceSnapshotService.saveProcessSnapshot(processo, analysis.strategicFocus(), analysis.institutionalPolicySnapshot(), analysis.kernelRiskEscalation(), analysis.governedMessageDecision());
        processIntelligenceSnapshotService.saveNegotiationRound(processo, proposta, usuarioSistema, analysis.governedMessageDecision(), analysis.strategicFocus(), analysis.suggestedNextMessage());
        chatService.postarMensagemSistema(processo, usuarioSistema, analysis.intelligenceDigest());
    }

    private void warmDetachedReferences(Processo processo, PropostaAcordo proposta, IaSettings settings) {
        processo.getId();
        if (processo.getModulo() != null) {
            processo.getModulo().name();
        }
        if (processo.getJurisdicao() != null && processo.getJurisdicao().getEsfera() != null) {
            processo.getJurisdicao().getEsfera().name();
        }
        if (processo.getFaseAtual() != null) {
            processo.getFaseAtual().name();
        }
        if (processo.getStatusProcesso() != null) {
            processo.getStatusProcesso().name();
        }
        proposta.getId();
        proposta.getUuid();
        proposta.getValorAcordo();
        proposta.getTermosHtml();
        settings.isSuggestionsEnabled();
    }

    private List<ChatMensagem> recentChat(Long processoId) {
        return chatMensagemRepository.findTop80ByProcesso_IdOrderByDataEnvioDesc(processoId).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChatMensagem::getDataEnvio, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(20)
                .toList();
    }

    private IARunResult gerarRunSafeDeAcordo(Processo processo, PropostaAcordo proposta, IaSettings settings, Profile profile) {
        try {
            var req = com.tcc.pjb.backend.ai.contract.IARequest.builder()
                    .origem("ACORDO")
                    .acao("GERAR_ACORDO")
                    .payload("processoId", processo.getId())
                    .payload("termosHtml", proposta.getTermosHtml())
                    .payload("profile", profile != null ? profile.getNome() : null)
                    .payload("suggestionsEnabled", settings.isSuggestionsEnabled())
                    .build();

            var resp = iaOrchestrator.processar(req);
            String html = resp != null && resp.getTexto() != null && !resp.getTexto().isBlank()
                    ? resp.getTexto()
                    : proposta.getTermosHtml();

            return new IARunResult(java.util.UUID.randomUUID(), html, 0.55, "LOW_RISK");
        } catch (Exception e) {
            log.warn("Orquestrador indisponível para acordo; seguindo com fallback seguro. processoId={}", processo.getId(), e);
            return new IARunResult(java.util.UUID.randomUUID(), proposta.getTermosHtml(), 0.40, "FALLBACK");
        }
    }

    private String buildIntelligenceDigest(SettlementAdvisoryReport settlementAdvisory,
                                           InstitutionalGovernanceContextReport governance,
                                           NegotiationMemoryReport negotiationMemory,
                                           NegotiationExplainabilityReport negotiationExplainability,
                                           KernelOperationalGovernanceReport kernelOperationalGovernance,
                                           NegotiationChatDigestReport negotiationChatDigest,
                                           NegotiationApprovalMatrixReport negotiationApprovalMatrix,
                                           NegotiationChannelGovernanceReport negotiationChannelGovernance,
                                           InstitutionalPolicySnapshotReport institutionalPolicySnapshot,
                                           KernelDecisionMetricsReport kernelDecisionMetrics,
                                           KernelRiskEscalationReport kernelRiskEscalation,
                                           NegotiationMessageDecision governedMessageDecision,
                                           KernelAdvisoryTelemetry telemetry) {
        LinkedHashSet<String> lines = new LinkedHashSet<>();
        lines.add("Snapshot de inteligência negocial atualizado.");
        if (settlementAdvisory != null) {
            lines.addAll(limit(settlementAdvisory.nextMoves(), 2));
            lines.addAll(limit(settlementAdvisory.executionSafeguards(), 2));
        }
        if (governance != null) {
            lines.addAll(limit(governance.policyGuards(), 2));
            lines.addAll(limit(governance.governanceAlerts(), 2));
        }
        if (negotiationMemory != null) {
            lines.addAll(limit(negotiationMemory.reusablePlaybooks(), 2));
            lines.addAll(limit(negotiationMemory.cautionPoints(), 2));
        }
        if (negotiationExplainability != null) {
            lines.addAll(limit(negotiationExplainability.openQuestions(), 2));
        }
        if (kernelOperationalGovernance != null) {
            lines.addAll(limit(kernelOperationalGovernance.nextActions(), 2));
            lines.addAll(limit(kernelOperationalGovernance.watchpoints(), 2));
        }
        if (negotiationChatDigest != null) {
            lines.add("Chat: " + negotiationChatDigest.conversationStage() + " | postura=" + negotiationChatDigest.posture() + " | temperatura=" + negotiationChatDigest.counterpartyTemperature() + " | envio=" + negotiationChatDigest.sendMode());
            lines.addAll(limit(negotiationChatDigest.anchorNarratives(), 2));
            lines.addAll(limit(negotiationChatDigest.protectedTopics(), 2));
            lines.addAll(limit(negotiationChatDigest.escalationSignals(), 2));
            lines.addAll(limit(negotiationChatDigest.nextTurnObjectives(), 2));
            lines.addAll(limit(negotiationChatDigest.forbiddenMoves(), 1));
            lines.addAll(limit(negotiationChatDigest.internalActions(), 2));
            lines.addAll(limit(negotiationChatDigest.messageBlueprints(), 1));
            if (negotiationChatDigest.suggestedNextMessage() != null && !negotiationChatDigest.suggestedNextMessage().isBlank()) {
                lines.add(negotiationChatDigest.suggestedNextMessage());
            }
        }
        if (negotiationApprovalMatrix != null) {
            lines.add("Approval: " + negotiationApprovalMatrix.approvalBand() + " | release=" + negotiationApprovalMatrix.releaseMode());
            lines.addAll(limit(negotiationApprovalMatrix.approvalGates(), 2));
            lines.addAll(limit(negotiationApprovalMatrix.escalationLanes(), 2));
            lines.addAll(limit(negotiationApprovalMatrix.internalControls(), 2));
            lines.addAll(limit(negotiationApprovalMatrix.releaseChecklist(), 2));
        }
        if (negotiationChannelGovernance != null) {
            lines.add("Canal: " + negotiationChannelGovernance.operatingMode() + " | persist=" + negotiationChannelGovernance.persistenceMode() + " | handshake=" + negotiationChannelGovernance.approvalHandshake());
            lines.addAll(limit(negotiationChannelGovernance.participantDirectives(), 2));
            lines.addAll(limit(negotiationChannelGovernance.releaseBoundaries(), 2));
            lines.addAll(limit(negotiationChannelGovernance.auditDirectives(), 2));
            lines.addAll(limit(negotiationChannelGovernance.memoryDirectives(), 2));
            lines.addAll(limit(negotiationChannelGovernance.deliveryGuardrails(), 2));
            lines.addAll(limit(negotiationChannelGovernance.fallbackLanes(), 2));
        }
        if (institutionalPolicySnapshot != null) {
            lines.add("Policy: " + institutionalPolicySnapshot.policyTier() + " | key=" + institutionalPolicySnapshot.policyKey() + " | strict=" + institutionalPolicySnapshot.strictRelease());
            if (institutionalPolicySnapshot.policyAxes() != null) {
                lines.add("Axes: mode=" + institutionalPolicySnapshot.policyAxes().selectionMode()
                        + " | ramo=" + institutionalPolicySnapshot.policyAxes().ramoDireito()
                        + " | materia=" + institutionalPolicySnapshot.policyAxes().materia()
                        + " | rito=" + institutionalPolicySnapshot.policyAxes().ritoProcessual());
                lines.addAll(limit(institutionalPolicySnapshot.policyAxes().matchedAxes(), 3));
            }
            lines.addAll(limit(institutionalPolicySnapshot.mandatoryDirectives(), 2));
            lines.addAll(limit(institutionalPolicySnapshot.blockingDirectives(), 2));
            lines.addAll(limit(institutionalPolicySnapshot.releaseGuardrails(), 2));
        }
        if (kernelDecisionMetrics != null) {
            lines.add("Metrics: total=" + kernelDecisionMetrics.totalDecisions() + " | blocked=" + kernelDecisionMetrics.blockedDecisions() + " | approval=" + kernelDecisionMetrics.approvalRequiredDecisions());
            lines.addAll(limit(kernelDecisionMetrics.hotSignals(), 2));
            lines.addAll(limit(kernelDecisionMetrics.stabilitySignals(), 2));
        }
        if (kernelRiskEscalation != null) {
            lines.add("Risk: level=" + kernelRiskEscalation.escalationLevel());
            lines.addAll(limit(kernelRiskEscalation.containmentActions(), 2));
            lines.addAll(limit(kernelRiskEscalation.recommendedLanes(), 2));
        }
        if (governedMessageDecision != null) {
            lines.add("Release: code=" + governedMessageDecision.decisionCode() + " | allowed=" + governedMessageDecision.releaseAllowed() + " | risk=" + governedMessageDecision.riskLevel());
            lines.addAll(limit(governedMessageDecision.reasons(), 2));
            lines.addAll(limit(governedMessageDecision.mandatoryActions(), 2));
        }
        if (telemetry != null) {
            lines.add("Telemetria: " + telemetry.statusBand() + " | componentes=" + telemetry.advisoryCount() + " | bloqueios=" + telemetry.blockingCount());
        }
        return String.join(" | ", lines);
    }

    private List<String> buildStrategicFocus(SettlementAdvisoryReport settlementAdvisory,
                                           InstitutionalGovernanceContextReport governance,
                                           NegotiationMemoryReport negotiationMemory,
                                           NegotiationExplainabilityReport negotiationExplainability,
                                           KernelOperationalGovernanceReport kernelOperationalGovernance,
                                           NegotiationChatDigestReport negotiationChatDigest,
                                           NegotiationApprovalMatrixReport negotiationApprovalMatrix,
                                           NegotiationChannelGovernanceReport negotiationChannelGovernance,
                                           InstitutionalPolicySnapshotReport institutionalPolicySnapshot,
                                           KernelDecisionMetricsReport kernelDecisionMetrics,
                                           KernelRiskEscalationReport kernelRiskEscalation,
                                           NegotiationMessageDecision governedMessageDecision) {
        LinkedHashSet<String> focus = new LinkedHashSet<>();
        if (settlementAdvisory != null) { focus.addAll(settlementAdvisory.nextMoves()); focus.addAll(settlementAdvisory.executionSafeguards()); }
        if (governance != null) { focus.addAll(governance.policyGuards()); focus.addAll(governance.governanceAlerts()); }
        if (negotiationMemory != null) { focus.addAll(negotiationMemory.reusablePlaybooks()); focus.addAll(negotiationMemory.cautionPoints()); }
        if (negotiationExplainability != null) { focus.addAll(negotiationExplainability.openQuestions()); }
        if (kernelOperationalGovernance != null) { focus.addAll(kernelOperationalGovernance.nextActions()); focus.addAll(kernelOperationalGovernance.watchpoints()); }
        if (negotiationChatDigest != null) { focus.addAll(negotiationChatDigest.anchorNarratives()); focus.addAll(negotiationChatDigest.protectedTopics()); focus.addAll(negotiationChatDigest.internalActions()); }
        if (negotiationApprovalMatrix != null) { focus.addAll(negotiationApprovalMatrix.approvalGates()); focus.addAll(negotiationApprovalMatrix.releaseChecklist()); }
        if (negotiationChannelGovernance != null) { focus.addAll(negotiationChannelGovernance.participantDirectives()); focus.addAll(negotiationChannelGovernance.deliveryGuardrails()); }
        if (institutionalPolicySnapshot != null) {
            focus.add(institutionalPolicySnapshot.policyKey());
            focus.add(institutionalPolicySnapshot.policyTier());
            if (institutionalPolicySnapshot.policyAxes() != null) {
                focus.add(institutionalPolicySnapshot.policyAxes().selectionMode());
                focus.addAll(institutionalPolicySnapshot.policyAxes().matchedAxes());
                focus.addAll(institutionalPolicySnapshot.policyAxes().declaredAxes());
            }
            focus.addAll(institutionalPolicySnapshot.mandatoryDirectives());
            focus.addAll(institutionalPolicySnapshot.blockingDirectives());
            focus.addAll(institutionalPolicySnapshot.releaseGuardrails());
            focus.addAll(institutionalPolicySnapshot.escalationTriggers());
        }
        if (kernelDecisionMetrics != null) { focus.addAll(kernelDecisionMetrics.hotSignals()); focus.addAll(kernelDecisionMetrics.stabilitySignals()); }
        if (kernelRiskEscalation != null) { focus.addAll(kernelRiskEscalation.containmentActions()); focus.addAll(kernelRiskEscalation.recommendedLanes()); }
        if (governedMessageDecision != null) { focus.addAll(governedMessageDecision.reasons()); focus.addAll(governedMessageDecision.mandatoryActions()); }
        focus.removeIf(s -> s == null || s.isBlank());
        return List.copyOf(focus);
    }

    private static List<String> buildNegotiationSignals(Processo processo) {
        List<String> out = new ArrayList<>();
        if (processo == null) {
            return List.of();
        }
        if (processo.getFaseAtual() != null) {
            out.add("Fase atual: " + processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            out.add("Status do processo: " + processo.getStatusProcesso().name());
        }
        return List.copyOf(out);
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty() || max <= 0) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(max).toList();
    }

    private Usuario buscarUsuarioSistema() {
        return usuarioRepository.findByTipoUsuario(com.tcc.pjb.backend.model.entity.enums.TipoUsuario.ADMINISTRADOR)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Usuário ADMINISTRADOR do sistema não configurado."));
    }

    private static com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia mapCompetencia(Processo processo) {
        if (processo == null || processo.getJurisdicao() == null) {
            return com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.ESTADUAL;
        }
        return mapCompetencia(processo.getJurisdicao().getEsfera());
    }

    private record AcordoPipelineSeed(Long propostaId,
                                      Long processoId,
                                      PropostaAcordo proposta,
                                      Processo processo,
                                      IaSettings settings,
                                      String ritoName,
                                      List<ChatMensagem> recentChat) {
    }

    private record AcordoPipelineOutcome(IARunResult run,
                                         EssenceResult decision,
                                         boolean contemClausulasAbusivas,
                                         boolean revisaoHumana,
                                         StatusAcordo status,
                                         String motivo,
                                         String mensagemSistema) {
    }

    private record AcordoPipelineAnalysis(SettlementAdvisoryReport settlementAdvisory,
                                          InstitutionalGovernanceContextReport governance,
                                          NegotiationMemoryReport negotiationMemory,
                                          NegotiationExplainabilityReport negotiationExplainability,
                                          KernelOperationalGovernanceReport kernelOperationalGovernance,
                                          NegotiationChatDigestReport negotiationChatDigest,
                                          NegotiationApprovalMatrixReport negotiationApprovalMatrix,
                                          NegotiationChannelGovernanceReport negotiationChannelGovernance,
                                          InstitutionalPolicySnapshotReport institutionalPolicySnapshot,
                                          KernelDecisionMetricsReport kernelDecisionMetrics,
                                          KernelRiskEscalationReport kernelRiskEscalation,
                                          NegotiationMessageDecision governedMessageDecision,
                                          KernelAdvisoryTelemetry telemetry,
                                          List<String> strategicFocus,
                                          String intelligenceDigest,
                                          String suggestedNextMessage) {
    }

    private static com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia mapCompetencia(com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao esfera) {
        if (esfera == null) {
            return com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.ESTADUAL;
        }
        return switch (esfera) {
            case JUSTICA_FEDERAL -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.FEDERAL;
            case JUSTICA_TRABALHO -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.TRABALHISTA;
            case JUSTICA_ELEITORAL -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.ELEITORAL;
            case JUSTICA_MILITAR -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.MILITAR;
            case JUSTICA_ESTADUAL -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.ESTADUAL;
            default -> com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia.TRIBUNAL_SUPERIOR;
        };
    }
}
