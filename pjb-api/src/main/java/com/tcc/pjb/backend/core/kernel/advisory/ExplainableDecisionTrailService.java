package com.tcc.pjb.backend.core.kernel.advisory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;

@Service
public class ExplainableDecisionTrailService {

    public ExplainableDecisionTrailReport composeRequest(LaianePeticaoAssistRequest request,
                                                         CanonicalContext canonical,
                                                         String ritoName,
                                                         DynamicCompetenceDistributionResponse competencia,
                                                         LegalCoherenceReport coherence,
                                                         ProtocolDryRunReport dryRun,
                                                         ProcessIntegrityRadarReport radar,
                                                         StrategicCopilotReport copilot,
                                                         InstitutionalMemoryReport memory,
                                                         ContextualPrecedentAdvisoryReport precedents) {
        Objects.requireNonNull(request, "request");
        List<ExplainableDecisionTrailReport.DecisionNode> nodes = new ArrayList<>();
        Set<String> openQuestions = new LinkedHashSet<>();
        double confidence = 0.59d;

        nodes.add(node(
                "CANONICAL_CONTEXT",
                "Fechamento do contexto canônico",
                "ProceduralCanonicalResolver",
                canonical != null ? "HIGH" : "LOW",
                List.of(valueOrPlaceholder(request.getClasseTpu()), valueOrPlaceholder(request.getRamoDireito()), valueOrPlaceholder(request.getMateriaPrincipal())),
                List.of(valueOrPlaceholder(ritoName), canonical != null ? valueOrPlaceholder(canonical.classeTpuCodigo()) : "classeTpu:pending"),
                canonical == null ? List.of("Contexto canônico não foi consolidado integralmente.") : List.of()
        ));

        nodes.add(node(
                "COHERENCE_ENGINE",
                "Motor de coerência jurídica",
                "LegalCoherenceEngine",
                coherence != null && !coherence.blocking() ? "MEDIUM_HIGH" : "MEDIUM_LOW",
                coherence != null ? coherence.issues().stream().map(LegalCoherenceReport.Issue::title).toList() : List.of("coherence:pending"),
                coherence != null ? coherence.strategicRecommendations() : List.of("recommendations:pending"),
                coherence != null && coherence.blocking() ? List.of("Existem incoerências bloqueantes a sanear.") : List.of()
        ));

        nodes.add(node(
                "COMPETENCE_AND_PROTOCOL",
                "Competência e ensaio de protocolo",
                "MapaCompetenciaDinamicoEngine/ProtocolDryRunService",
                dryRun != null && dryRun.apto() ? "MEDIUM_HIGH" : "MEDIUM_LOW",
                List.of(
                        competencia != null ? valueOrPlaceholder(competencia.tribunalCodigo()) : "tribunal:pending",
                        competencia != null ? valueOrPlaceholder(competenceStatus(competencia)) : "competencia:pending",
                        dryRun != null ? valueOrPlaceholder(dryRun.status()) : "dryRun:pending"
                ),
                dryRun != null ? dryRun.nextActions() : List.of("protocol:pending"),
                dryRun != null && !dryRun.apto() ? dryRun.findings().stream().map(ProtocolDryRunReport.Finding::message).limit(3).toList() : List.of()
        ));

        nodes.add(node(
                "INTEGRITY_AND_STRATEGY",
                "Radar de integridade e copiloto estratégico",
                "ProcessIntegrityRadarService/StrategicCopilotService",
                radar != null && !radar.blocking() ? "MEDIUM" : "MEDIUM_LOW",
                radar != null ? radar.watchpoints() : List.of("integrity:pending"),
                copilot != null ? copilot.watchpoints() : List.of("copilot:pending"),
                radar != null && radar.blocking() ? radar.nextActions() : List.of()
        ));

        nodes.add(node(
                "MEMORY_AND_PRECEDENTS",
                "Memória institucional e precedentes contextuais",
                "InstitutionalMemoryService/ContextualPrecedentAdvisoryService",
                memory != null && precedents != null ? "MEDIUM" : "LOW",
                merge(memory != null ? memory.memoryKeys() : List.of(), precedents != null ? precedents.anchorDimensions() : List.of()),
                merge(memory != null ? memory.reusablePlaybooks() : List.of(), precedents != null ? precedents.recommendedQueries() : List.of()),
                merge(memory != null ? memory.repeatedFailureModes() : List.of(), precedents != null ? precedents.cautionPoints() : List.of())
        ));

        if (canonical == null) {
            openQuestions.add("Confirmar classe TPU, ramo e rito efetivo antes do protocolo institucional.");
            confidence -= 0.11d;
        }
        if (competencia == null || !competencia.distribuicaoAutomatica()) {
            openQuestions.add("Fechar tribunal, unidade julgadora e distribuição com sinal estável.");
            confidence -= 0.08d;
        }
        if (coherence != null && coherence.blocking()) {
            openQuestions.add("Sanear incoerência jurídica bloqueante antes de consolidar a peça.");
            confidence -= 0.10d;
        }
        if (radar != null && radar.blocking()) {
            openQuestions.addAll(limit(radar.nextActions(), 3));
            confidence -= 0.08d;
        }
        if (memory != null && !memory.repeatedFailureModes().isEmpty()) {
            openQuestions.addAll(limit(memory.repeatedFailureModes(), 2));
        }
        if (precedents != null && !precedents.cautionPoints().isEmpty()) {
            openQuestions.addAll(limit(precedents.cautionPoints(), 2));
        }

        return new ExplainableDecisionTrailReport(
                "PETITION_ASSIST",
                openQuestions.isEmpty() ? "EXPLAINABILITY_STABLE" : "EXPLAINABILITY_ATTENTION",
                round(clamp(confidence)),
                List.copyOf(nodes),
                List.copyOf(openQuestions),
                PayloadMaps.ofEntries(
                        "scope", "PETITION_ASSIST",
                        "ritoName", ritoName,
                        "classeTpu", canonical != null ? canonical.classeTpuCodigo() : null,
                        "competenciaStatus", competencia != null ? competenceStatus(competencia) : null,
                        "dryRunStatus", dryRun != null ? dryRun.status() : null
                )
        );
    }

    public ExplainableDecisionTrailReport composeProcess(Processo processo,
                                                         String ritoName,
                                                         RitoPlanDto ritoPlan,
                                                         LegalCoherenceReport coherence,
                                                         ProtocolDryRunReport dryRun,
                                                         ProcessIntegrityRadarReport radar,
                                                         StrategicCopilotReport copilot,
                                                         InstitutionalMemoryReport memory,
                                                         ContextualPrecedentAdvisoryReport precedents,
                                                         SettlementAdvisoryReport settlement) {
        Objects.requireNonNull(processo, "processo");
        List<ExplainableDecisionTrailReport.DecisionNode> nodes = new ArrayList<>();
        Set<String> openQuestions = new LinkedHashSet<>();
        double confidence = 0.64d;

        nodes.add(node(
                "PROCESS_BASELINE",
                "Linha-base do processo",
                "ProcessDigitalTwinService",
                !blank(ritoName) ? "HIGH" : "MEDIUM_LOW",
                List.of(valueOrPlaceholder(processo.getNumeroUnificado()), valueOrPlaceholder(ritoName), processo.getFaseAtual() != null ? processo.getFaseAtual().name() : "fase:pending"),
                ritoPlan != null ? List.of("workflow-ready") : List.of("workflow:pending"),
                blank(ritoName) ? List.of("Rito efetivo ainda não está disponível na leitura do gêmeo digital.") : List.of()
        ));

        nodes.add(node(
                "PROCESS_COHERENCE",
                "Coerência e prontidão operacional",
                "LegalCoherenceEngine/ProtocolDryRunService",
                dryRun != null && dryRun.apto() ? "MEDIUM_HIGH" : "MEDIUM_LOW",
                coherence != null ? coherence.issues().stream().map(LegalCoherenceReport.Issue::title).toList() : List.of("coherence:pending"),
                dryRun != null ? dryRun.nextActions() : List.of("dryRun:pending"),
                coherence != null && coherence.blocking() ? coherence.issues().stream().map(LegalCoherenceReport.Issue::title).toList() : List.of()
        ));

        nodes.add(node(
                "PROCESS_INTEGRITY",
                "Radar de integridade",
                "ProcessIntegrityRadarService",
                radar != null && !radar.blocking() ? "MEDIUM" : "MEDIUM_LOW",
                radar != null ? radar.watchpoints() : List.of("radar:pending"),
                radar != null ? radar.nextActions() : List.of("integrity-actions:pending"),
                radar != null && radar.blocking() ? radar.findings().stream().map(ProcessIntegrityRadarReport.Finding::message).limit(3).toList() : List.of()
        ));

        nodes.add(node(
                "PROCESS_MEMORY_AND_PRECEDENTS",
                "Memória institucional e precedentes",
                "InstitutionalMemoryService/ContextualPrecedentAdvisoryService",
                precedents != null ? "MEDIUM" : "LOW",
                merge(memory != null ? memory.learnedPatterns() : List.of(), precedents != null ? precedents.anchorDimensions() : List.of()),
                merge(memory != null ? memory.reusablePlaybooks() : List.of(), precedents != null ? precedents.recommendedQueries() : List.of()),
                merge(memory != null ? memory.officeAlerts() : List.of(), precedents != null ? precedents.cautionPoints() : List.of())
        ));

        nodes.add(node(
                "PROCESS_SETTLEMENT_STRATEGY",
                "Executabilidade e autocomposição",
                "SettlementAdvisoryService/StrategicCopilotService",
                settlement != null && settlement.window() != null && settlement.window().favorable() ? "MEDIUM_HIGH" : "MEDIUM",
                settlement != null ? settlement.executionSafeguards() : List.of("settlement:pending"),
                copilot != null ? copilot.negotiationActions().stream().map(StrategicCopilotReport.Action::title).toList() : List.of("negotiation:pending"),
                settlement != null && !settlement.executable() ? settlementRisks(settlement) : List.of()
        ));

        if (blank(ritoName)) {
            openQuestions.add("Consolidar rito efetivo do processo antes de usar o twin como trilha institucional plena.");
            confidence -= 0.1d;
        }
        if (ritoPlan != null && ritoPlan.getBlockingOpen() != null && !ritoPlan.getBlockingOpen().isEmpty()) {
            openQuestions.add("Sanear work items bloqueantes e revalidar transição de fase.");
            confidence -= 0.08d;
        }
        if (radar != null && radar.blocking()) {
            openQuestions.addAll(limit(radar.nextActions(), 3));
            confidence -= 0.08d;
        }
        if (settlement != null && !settlement.executable()) {
            openQuestions.addAll(limit(settlement.executionSafeguards(), 2));
            confidence -= 0.05d;
        }

        return new ExplainableDecisionTrailReport(
                "PROCESS_TWIN",
                openQuestions.isEmpty() ? "PROCESS_EXPLAINABILITY_STABLE" : "PROCESS_EXPLAINABILITY_ATTENTION",
                round(clamp(confidence)),
                List.copyOf(nodes),
                List.copyOf(openQuestions),
                PayloadMaps.ofEntries(
                        "scope", "PROCESS_TWIN",
                        "processoId", processo.getId(),
                        "ritoName", ritoName,
                        "faseAtual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                        "dryRunStatus", dryRun != null ? dryRun.status() : null
                )
        );
    }

    private static String competenceStatus(DynamicCompetenceDistributionResponse competencia) {
        if (competencia == null) {
            return null;
        }
        return competencia.distribuicaoAutomatica() ? "COMPETENCE_RESOLVED" : "COMPETENCE_REVIEW";
    }

    private static List<String> settlementRisks(SettlementAdvisoryReport settlement) {
        if (settlement == null || settlement.window() == null || settlement.window().risks() == null) {
            return List.of();
        }
        return settlement.window().risks();
    }

    private static ExplainableDecisionTrailReport.DecisionNode node(String code,
                                                                    String title,
                                                                    String source,
                                                                    String confidenceBand,
                                                                    List<String> inputs,
                                                                    List<String> outputs,
                                                                    List<String> risks) {
        return new ExplainableDecisionTrailReport.DecisionNode(
                code,
                title,
                source,
                confidenceBand,
                List.copyOf(limit(inputs, 6)),
                List.copyOf(limit(outputs, 6)),
                List.copyOf(limit(risks, 6))
        );
    }

    private static List<String> merge(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(limit(first, 4));
        merged.addAll(limit(second, 4));
        return List.copyOf(merged);
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).limit(max).toList();
    }

    private static String valueOrPlaceholder(String value) {
        return value == null || value.isBlank() ? "pending" : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static double clamp(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    private static double round(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
