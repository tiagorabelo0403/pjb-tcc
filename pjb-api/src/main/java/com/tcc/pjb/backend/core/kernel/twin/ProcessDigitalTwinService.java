package com.tcc.pjb.backend.core.kernel.twin;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.advisory.ContextualPrecedentAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.ContextualPrecedentAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.ExplainableDecisionTrailReport;
import com.tcc.pjb.backend.core.kernel.advisory.ExplainableDecisionTrailService;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextService;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalMemoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalMemoryService;
import com.tcc.pjb.backend.core.kernel.advisory.KernelAdvisoryTelemetry;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceService;
import com.tcc.pjb.backend.core.kernel.advisory.LegalCoherenceEngine;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessIntegrityRadarService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.ProtocolDryRunService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.StrategicCopilotService;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.procedural.ProceduralRitoNames;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.twin.PrecedenteEvidenceDto;
import com.tcc.pjb.backend.model.dto.twin.ProcessTwinDto;
import com.tcc.pjb.backend.model.dto.twin.TwinRecommendationDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.PrecedenteRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import com.tcc.pjb.backend.service.rito.RitoWorkflowService;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;
import com.tcc.pjb.backend.service.semantic.SemanticPrecedentSearchService;

@Service
public class ProcessDigitalTwinService {

    private static final String KERNEL_VERSION = "PJB-KERNEL/2026.1";

    private final ProcessoRepository processoRepository;
    private final PrecedenteRepository precedenteRepository;
    private final ProcessEventStore processEventStore;
    private final RitoWorkflowService ritoWorkflowService;
    private final PjbAuthorizationService authorizationService;
    private final SemanticPrecedentSearchService semanticSearch;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;
    private final LegalCoherenceEngine legalCoherenceEngine;
    private final ProtocolDryRunService protocolDryRunService;
    private final ProcessIntegrityRadarService processIntegrityRadarService;
    private final StrategicCopilotService strategicCopilotService;
    private final SettlementAdvisoryService settlementAdvisoryService;
    private final ProcessMaterialDossierService processMaterialDossierService;
    private final ProcessMaterialStrategyService processMaterialStrategyService;
    private final InstitutionalMemoryService institutionalMemoryService;
    private final InstitutionalGovernanceContextService institutionalGovernanceContextService;
    private final ContextualPrecedentAdvisoryService contextualPrecedentAdvisoryService;
    private final ExplainableDecisionTrailService explainableDecisionTrailService;
    private final KernelOperationalGovernanceService kernelOperationalGovernanceService;
    private final ProcessTwinProceduralOrchestrator proceduralOrchestrator;
    private final ProcessTwinNegotiationOrchestrator negotiationOrchestrator;
    private final ProcessTwinPolicyGovernanceOrchestrator policyGovernanceOrchestrator;

    public ProcessDigitalTwinService(ProcessoRepository processoRepository,
                                     PrecedenteRepository precedenteRepository,
                                     ProcessEventStore processEventStore,
                                     RitoWorkflowService ritoWorkflowService,
                                     PjbAuthorizationService authorizationService,
                                     SemanticPrecedentSearchService semanticSearch,
                                     ProcessoRitoSnapshotService processoRitoSnapshotService,
                                     LegalCoherenceEngine legalCoherenceEngine,
                                     ProtocolDryRunService protocolDryRunService,
                                     ProcessIntegrityRadarService processIntegrityRadarService,
                                     StrategicCopilotService strategicCopilotService,
                                     SettlementAdvisoryService settlementAdvisoryService,
                                     ProcessMaterialDossierService processMaterialDossierService,
                                     ProcessMaterialStrategyService processMaterialStrategyService,
                                     InstitutionalMemoryService institutionalMemoryService,
                                     InstitutionalGovernanceContextService institutionalGovernanceContextService,
                                     ContextualPrecedentAdvisoryService contextualPrecedentAdvisoryService,
                                     ExplainableDecisionTrailService explainableDecisionTrailService,
                                     KernelOperationalGovernanceService kernelOperationalGovernanceService,
                                     ProcessTwinProceduralOrchestrator proceduralOrchestrator,
                                     ProcessTwinNegotiationOrchestrator negotiationOrchestrator,
                                     ProcessTwinPolicyGovernanceOrchestrator policyGovernanceOrchestrator) {
        this.processoRepository = processoRepository;
        this.precedenteRepository = precedenteRepository;
        this.processEventStore = processEventStore;
        this.ritoWorkflowService = ritoWorkflowService;
        this.authorizationService = authorizationService;
        this.semanticSearch = semanticSearch;
        this.processoRitoSnapshotService = processoRitoSnapshotService;
        this.legalCoherenceEngine = legalCoherenceEngine;
        this.protocolDryRunService = protocolDryRunService;
        this.processIntegrityRadarService = processIntegrityRadarService;
        this.strategicCopilotService = strategicCopilotService;
        this.settlementAdvisoryService = settlementAdvisoryService;
        this.processMaterialDossierService = processMaterialDossierService;
        this.processMaterialStrategyService = processMaterialStrategyService;
        this.institutionalMemoryService = institutionalMemoryService;
        this.institutionalGovernanceContextService = institutionalGovernanceContextService;
        this.contextualPrecedentAdvisoryService = contextualPrecedentAdvisoryService;
        this.explainableDecisionTrailService = explainableDecisionTrailService;
        this.kernelOperationalGovernanceService = kernelOperationalGovernanceService;
        this.proceduralOrchestrator = proceduralOrchestrator;
        this.negotiationOrchestrator = negotiationOrchestrator;
        this.policyGovernanceOrchestrator = policyGovernanceOrchestrator;
    }

    @Transactional(readOnly = true)
    public ProcessTwinDto twin(Long processoId) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");

        Processo p = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));

        authorizationService.requireReadProcesso(p);

        var events = processEventStore.stream(processoId);
        LocalDateTime lastEventAt = events.stream()
                .map(event -> event.getCreatedAt())
                .filter(java.util.Objects::nonNull)
                .map(ts -> java.time.LocalDateTime.ofInstant(ts, java.time.ZoneOffset.UTC))
                .max(java.time.LocalDateTime::compareTo)
                .orElse(null);

        RitoPlanDto ritoPlan = ritoWorkflowService.plan(processoId);
        var ritoSnapshot = processoRitoSnapshotService.resolve(p, null);

        List<PrecedenteEvidenceDto> evidence = loadEvidence(p, ritoSnapshot.ritoCode());
        List<String> riskSignals = buildRiskSignals(p, ritoPlan, evidence);
        List<TwinRecommendationDto> recs = buildRecommendations(p, ritoSnapshot.ritoCode(), ritoPlan, evidence);
        var coherenceReport = legalCoherenceEngine.analyzeProcess(p, ritoSnapshot.ritoCode(), ritoPlan, riskSignals, !evidence.isEmpty());
        var protocolDryRun = protocolDryRunService.simulateProcess(ritoSnapshot.ritoCode(), p.getId(), p.getNumeroUnificado(), ritoPlan, !evidence.isEmpty(), coherenceReport);
        var integrityRadar = processIntegrityRadarService.analyzeProcess(p, ritoSnapshot.ritoCode(), ritoPlan, coherenceReport, protocolDryRun, riskSignals);
        ProcessMaterialDossierReport materialDossier = processMaterialDossierService.analyzeProcess(p, riskSignals);
        ProcessMaterialStrategyReport materialStrategy = processMaterialStrategyService.analyzeProcess(p, materialDossier, riskSignals);
        ProcessTwinProceduralOrchestrator.Bundle proceduralBundle = proceduralOrchestrator.analyzeProcess(p);
        ProceduralRoutingReport proceduralRouting = proceduralBundle.routing();
        ProceduralSubmissionBlueprintReport submissionBlueprint = proceduralBundle.submissionBlueprint();
        ProceduralConnectorExecutionReport connectorExecution = proceduralBundle.connectorExecution();
        riskSignals = mergeRiskSignals(riskSignals, proceduralRouting, submissionBlueprint);
        riskSignals = mergeRiskSignals(riskSignals, connectorExecution);
        var settlementAdvisory = settlementAdvisoryService.analyze(p, ritoSnapshot.ritoCode(), p.getValorCausa(), mergeRiskSignals(riskSignals, materialDossier, materialStrategy), integrityRadar);
        var strategicCopilot = strategicCopilotService.analyzeProcess(p, ritoSnapshot.ritoCode(), ritoPlan, coherenceReport, protocolDryRun, integrityRadar, settlementAdvisory);
        InstitutionalMemoryReport institutionalMemory = institutionalMemoryService.analyzeProcess(p, ritoSnapshot.ritoCode(), ritoPlan, integrityRadar, strategicCopilot, settlementAdvisory);
        ContextualPrecedentAdvisoryReport precedentAdvisory = contextualPrecedentAdvisoryService.analyzeProcess(p, ritoSnapshot.ritoCode(), ritoPlan, evidence, settlementAdvisory, integrityRadar);
        ExplainableDecisionTrailReport explainableDecisionTrail = explainableDecisionTrailService.composeProcess(p, ritoSnapshot.ritoCode(), ritoPlan, coherenceReport, protocolDryRun, integrityRadar, strategicCopilot, institutionalMemory, precedentAdvisory, settlementAdvisory);
        InstitutionalGovernanceContextReport institutionalGovernanceContext = institutionalGovernanceContextService.analyzeProcess(p, ritoSnapshot.ritoCode(), settlementAdvisory, institutionalMemory, precedentAdvisory);
        ProcessTwinNegotiationOrchestrator.PreBundle negotiationPre = negotiationOrchestrator.prepareContext(p, settlementAdvisory, institutionalGovernanceContext);
        KernelOperationalGovernanceReport kernelOperationalGovernance = kernelOperationalGovernanceService.analyzeProcess(p, ritoSnapshot.ritoCode(), integrityRadar, explainableDecisionTrail, institutionalGovernanceContext, negotiationPre.memory(), negotiationPre.explainability(), strategicCopilot, institutionalMemory);
        ProcessTwinNegotiationOrchestrator.Bundle negotiationBundle = negotiationOrchestrator.finalizeAnalysis(p, negotiationPre, settlementAdvisory, institutionalGovernanceContext, kernelOperationalGovernance);
        var latestProposal = negotiationBundle.latestProposal();
        var recentChat = negotiationBundle.recentChat();
        var negotiationMemory = negotiationBundle.memory();
        var negotiationExplainability = negotiationBundle.explainability();
        var negotiationChatDigest = negotiationBundle.chatDigest();
        var negotiationApprovalMatrix = negotiationBundle.approvalMatrix();
        var negotiationChannelGovernance = negotiationBundle.channelGovernance();
        ProcessTwinPolicyGovernanceOrchestrator.Bundle policyBundle = policyGovernanceOrchestrator.analyzeProcess(p, ritoSnapshot.ritoCode(), latestProposal, recentChat, institutionalGovernanceContext, negotiationChatDigest, negotiationApprovalMatrix, negotiationChannelGovernance);
        var institutionalPolicySnapshot = policyBundle.policySnapshot();
        var kernelDecisionMetrics = policyBundle.decisionMetrics();
        var kernelRiskEscalation = policyBundle.riskEscalation();
        var governedMessageDecision = policyBundle.governedMessageDecision();
        KernelAdvisoryTelemetry advisoryTelemetry = kernelOperationalGovernanceService.buildTelemetry(
                "PROCESS_TWIN",
                ritoSnapshot.ritoCode(),
                coherenceReport,
                protocolDryRun,
                integrityRadar,
                materialDossier,
                materialStrategy,
                proceduralRouting,
                submissionBlueprint,
                connectorExecution,
                strategicCopilot,
                settlementAdvisory,
                institutionalMemory,
                precedentAdvisory,
                explainableDecisionTrail,
                institutionalGovernanceContext,
                negotiationMemory,
                negotiationExplainability,
                kernelOperationalGovernance,
                negotiationChannelGovernance,
                institutionalPolicySnapshot,
                kernelDecisionMetrics,
                kernelRiskEscalation,
                governedMessageDecision
        );

        return new ProcessTwinDto(
                KERNEL_VERSION,
                p.getId(),
                p.getNumeroUnificado(),
                p.getRamoDireito() == null ? null : p.getRamoDireito().name(),
                p.getMateria() == null ? null : p.getMateria().name(),
                ritoSnapshot.ritoCode(),
                p.getFaseAtual() == null ? null : p.getFaseAtual().name(),
                p.getNivelSigilo() == null ? null : p.getNivelSigilo().name(),
                p.getAssunto(),
                p.getObjetoProcessual(),
                p.getPedidoPrincipal(),
                p.getMaterialProbatorioResumo(),
                p.getMaterialProbatorioScore(),
                p.getPotencialAcordoScore(),
                p.getJanelaAcordoResumo(),
                p.getCatalogVersionId(),
                events.size(),
                lastEventAt,
                ritoPlan,
                recs,
                evidence,
                coherenceReport,
                protocolDryRun,
                integrityRadar,
                materialDossier,
                materialStrategy,
                proceduralRouting,
                submissionBlueprint,
                connectorExecution,
                strategicCopilot,
                settlementAdvisory,
                institutionalMemory,
                precedentAdvisory,
                explainableDecisionTrail,
                institutionalGovernanceContext,
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
                advisoryTelemetry,
                riskSignals
        );
    }

    private List<String> mergeRiskSignals(List<String> riskSignals, ProceduralRoutingReport proceduralRouting, ProceduralSubmissionBlueprintReport submissionBlueprint) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (riskSignals != null) {
            merged.addAll(riskSignals);
        }
        if (proceduralRouting != null) {
            merged.add(proceduralRouting.tipoJusticaSugerida());
            merged.add(proceduralRouting.ritoSugerido());
            merged.add(proceduralRouting.proceduralRegime());
            merged.add(proceduralRouting.proceduralTrack());
            merged.add(proceduralRouting.actionNature());
            merged.add(proceduralRouting.probatoryProfile());
            merged.add(proceduralRouting.complexityBand());
            merged.add(proceduralRouting.varaSugerida());
            merged.addAll(proceduralRouting.blockingIssues());
            merged.addAll(proceduralRouting.alerts());
            merged.addAll(proceduralRouting.reasons());
            merged.addAll(proceduralRouting.reviewChecklist());
            if (proceduralRouting.economicGate() != null) {
                merged.add(proceduralRouting.economicGate().economicBand());
                merged.add(proceduralRouting.economicGate().thresholdKind());
                merged.addAll(proceduralRouting.economicGate().reasons());
                merged.addAll(proceduralRouting.economicGate().rerouteOptions());
                merged.addAll(proceduralRouting.economicGate().reviewChecklist());
            }
            if (proceduralRouting.forumAllocation() != null) {
                merged.add(proceduralRouting.forumAllocation().classeTpuCodigo());
                merged.add(proceduralRouting.forumAllocation().competenciaTerritorialModo());
                merged.add(proceduralRouting.forumAllocation().preventionMode());
                merged.add(proceduralRouting.forumAllocation().linkageMode());
                merged.add(proceduralRouting.forumAllocation().tribunalCodigo());
                merged.add(proceduralRouting.forumAllocation().unidadeJudiciariaCodigo());
                merged.add(proceduralRouting.forumAllocation().connectorSystem());
                merged.add(proceduralRouting.forumAllocation().preProtocoloStatus());
                merged.addAll(proceduralRouting.forumAllocation().relatedProcessNumbers());
                merged.addAll(proceduralRouting.forumAllocation().incompatibilities());
                merged.addAll(proceduralRouting.forumAllocation().warnings());
                merged.addAll(proceduralRouting.forumAllocation().reviewChecklist());
            }
        }
        if (submissionBlueprint != null) {
            merged.add(submissionBlueprint.blueprintStatus());
            merged.add(submissionBlueprint.localCorrelationMode());
            merged.add(submissionBlueprint.tribunalCodigo());
            merged.add(submissionBlueprint.unidadeJudiciariaCodigo());
            merged.add(submissionBlueprint.judicialSystem() != null ? submissionBlueprint.judicialSystem().name() : null);
            merged.add(submissionBlueprint.dryRunStatus());
            merged.addAll(submissionBlueprint.relatedLocalProcessNumbers());
            merged.addAll(submissionBlueprint.blockingIssues());
            merged.addAll(submissionBlueprint.reviewChecklist());
            merged.addAll(submissionBlueprint.warnings());
        }
        merged.removeIf(s -> s == null || s.isBlank());
        return List.copyOf(merged);
    }

    private List<String> mergeRiskSignals(List<String> riskSignals, ProceduralConnectorExecutionReport connectorExecution) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (riskSignals != null) {
            merged.addAll(riskSignals);
        }
        if (connectorExecution != null) {
            merged.add(connectorExecution.executionMode());
            merged.add(connectorExecution.submissionLane());
            merged.add(connectorExecution.tribunalTargetKey());
            merged.add(connectorExecution.signerMode());
            merged.add(connectorExecution.retryPolicy());
            merged.addAll(connectorExecution.phases());
            merged.addAll(connectorExecution.executionChecklist());
            merged.addAll(connectorExecution.blockers());
            merged.addAll(connectorExecution.warnings());
        }
        merged.removeIf(s -> s == null || s.isBlank());
        return List.copyOf(merged);
    }

    private List<String> mergeRiskSignals(List<String> riskSignals, ProcessMaterialDossierReport materialDossier, ProcessMaterialStrategyReport materialStrategy) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (riskSignals != null) {
            merged.addAll(riskSignals);
        }
        if (materialDossier != null) {
            merged.addAll(materialDossier.proofGaps());
            merged.addAll(materialDossier.protocolChecklist());
        }
        if (materialStrategy != null) {
            merged.add(materialStrategy.litigationPosture());
            merged.add(materialStrategy.protocolReadiness());
            merged.add(materialStrategy.negotiationStance());
            merged.add(materialStrategy.evidenceReadiness());
            merged.addAll(materialStrategy.protocolBlockers());
            merged.addAll(materialStrategy.controlPoints());
        }
        merged.removeIf(s -> s == null || s.isBlank());
        return List.copyOf(merged);
    }

    private List<PrecedenteEvidenceDto> loadEvidence(Processo p, String ritoName) {
        if (p.getRamoDireito() == null && (ritoName == null || ritoName.isBlank()) && (p.getAssunto() == null || p.getAssunto().isBlank())) {
            return List.of();
        }

        String q = p.getAssunto() != null && !p.getAssunto().isBlank() ? p.getAssunto().trim() : null;

        var page = precedenteRepository.search(
                null,
                null,
                p.getRamoDireito(),
                ProceduralRitoNames.parse(ritoName),
                q,
                PageRequest.of(0, 12)
        );

        var base = page.getContent().stream().filter(Objects::nonNull).toList();
        if (!base.isEmpty()) {
            return base.stream().map(this::toEvidence).toList();
        }

        if (q == null || q.isBlank()) {
            return List.of();
        }

        var semantic = semanticSearch.semanticSearch(p.getRamoDireito(), ritoName, q, 12);
        return semantic.stream().filter(Objects::nonNull).map(this::toEvidence).toList();
    }

    private PrecedenteEvidenceDto toEvidence(Precedente p) {
        return new PrecedenteEvidenceDto(
                p.getId(),
                p.getFonte() == null ? null : p.getFonte().name(),
                p.getTipo() == null ? null : p.getTipo().name(),
                p.getIdentificador(),
                p.getTitulo(),
                p.getUrlReferencia(),
                p.getDataPublicacao()
        );
    }

    private List<String> buildRiskSignals(Processo processo, RitoPlanDto ritoPlan, List<PrecedenteEvidenceDto> evidence) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        if (processo.getFaseAtual() == null) {
            signals.add("Fase processual ausente no twin do processo.");
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            signals.add("Processo sob regime de sigilo reforçado com exigência de credencial.");
        }
        if (ritoPlan != null && ritoPlan.getBlockingOpen() != null && !ritoPlan.getBlockingOpen().isEmpty()) {
            signals.add("Existem pendências bloqueantes em aberto no workflow do rito.");
        }
        if (evidence == null || evidence.isEmpty()) {
            signals.add("Não foram localizados precedentes aderentes suficientes na trilha atual.");
        }
        if (processo.getConnectorSubmissionStatus() != null && !processo.getConnectorSubmissionStatus().isBlank()) {
            signals.add("Status do protocolo judicial: " + processo.getConnectorSubmissionStatus());
        }
        if (processo.getConnectorProtocolReference() != null && !processo.getConnectorProtocolReference().isBlank()) {
            signals.add("Recibo institucional vinculado ao processo: " + processo.getConnectorProtocolReference());
        }
        if (processo.getConnectorSyncStatus() != null && !processo.getConnectorSyncStatus().isBlank()) {
            signals.add("Estado de sincronização externa: " + processo.getConnectorSyncStatus());
        }
        if (processo.getConnectorSyncMessage() != null && !processo.getConnectorSyncMessage().isBlank()) {
            signals.add("Mensagem de sincronização: " + processo.getConnectorSyncMessage());
        }
        return List.copyOf(signals);
    }

    private List<TwinRecommendationDto> buildRecommendations(Processo processo,
                                                             String ritoName,
                                                             RitoPlanDto ritoPlan,
                                                             List<PrecedenteEvidenceDto> evidence) {
        List<TwinRecommendationDto> out = new ArrayList<>();

        if (ritoName == null || ritoName.isBlank()) {
            out.add(new TwinRecommendationDto(
                    "RITO_MISSING",
                    "Rito não definido",
                    "O processo ainda não possui rito processual definido. O PJB consegue sugerir um rito padrão pela jurisdição e matéria, mas a definição final deve ser confirmada para evitar fluxo inválido.",
                    "HIGH",
                    List.of()
            ));
        }

        if (processo.getFaseAtual() == null) {
            out.add(new TwinRecommendationDto(
                    "FASE_MISSING",
                    "Fase atual não definida",
                    "O processo não possui fase atual registrada. O motor de ritos depende da fase para validar transições e gerar checklist. Recomenda-se normalizar o estado pela timeline de movimentações.",
                    "HIGH",
                    List.of()
            ));
        }

        if (ritoPlan != null && ritoPlan.getBlockingOpen() != null && !ritoPlan.getBlockingOpen().isEmpty()) {
            out.add(new TwinRecommendationDto(
                    "CHECKLIST_BLOCKING",
                    "Há pendências que travam o avanço",
                    "O rito está impedido de avançar enquanto existir tarefa de checklist marcada como blocking em aberto.",
                    "HIGH",
                    List.of()
            ));
        }

        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            out.add(new TwinRecommendationDto(
                    "SIGILO_AUDIT",
                    "Acesso a processo sigiloso",
                    "Este processo exige credencial e justificativa conforme política ABAC. O PJB registra trilha de auditoria por request.",
                    "MEDIUM",
                    List.of()
            ));
        }

        if (evidence != null && !evidence.isEmpty()) {
            List<Long> ids = evidence.stream().map(PrecedenteEvidenceDto::id).filter(Objects::nonNull).limit(5).toList();
            out.add(new TwinRecommendationDto(
                    "JURIS_ANCHOR",
                    "Ancorar tese em precedentes recentes",
                    "Foram encontrados precedentes compatíveis com ramo, rito e assunto. O PJB recomenda citar os mais recentes e relevantes, sempre conferindo aderência fática.",
                    "LOW",
                    ids
            ));
        }

        return out.stream()
                .sorted(Comparator.comparingInt((TwinRecommendationDto r) -> severityScore(r.severity())).thenComparing(r -> r.code() == null ? "" : r.code()))
                .toList();
    }

    private int severityScore(String sev) {
        if (sev == null) {
            return 99;
        }
        return switch (sev) {
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 99;
        };
    }
}
