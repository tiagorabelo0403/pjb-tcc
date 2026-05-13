package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ProcessMaterialStrategyReportFactory {

    private final ProcessMaterialStrategyTextSupport textSupport;
    private final ProcessMaterialStrategyScoringPolicy scoringPolicy;
    private final ProcessMaterialStrategyMetricsFactory metricsFactory;
    private final ProcessMaterialStrategyControlPointFactory controlPointFactory;

    ProcessMaterialStrategyReportFactory(ProcessMaterialStrategyTextSupport textSupport,
                                         ProcessMaterialStrategyScoringPolicy scoringPolicy,
                                         ProcessMaterialStrategyMetricsFactory metricsFactory,
                                         ProcessMaterialStrategyControlPointFactory controlPointFactory) {
        this.textSupport = Objects.requireNonNull(textSupport);
        this.scoringPolicy = Objects.requireNonNull(scoringPolicy);
        this.metricsFactory = Objects.requireNonNull(metricsFactory);
        this.controlPointFactory = Objects.requireNonNull(controlPointFactory);
    }

    ProcessMaterialStrategyReport create(ProcessMaterialStrategyInput input) {
        ProcessMaterialDossierReport dossier = input.dossier();
        List<String> controversyAxes = dossier != null ? textSupport.safeList(dossier.controversyAxes()) : List.of();
        List<String> thesisVectors = dossier != null ? textSupport.safeList(dossier.thesisVectors()) : List.of();
        List<String> evidenceAnchors = dossier != null ? textSupport.safeList(dossier.evidenceAnchors()) : List.of();
        List<String> proofGaps = dossier != null ? textSupport.safeList(dossier.proofGaps()) : List.of();
        List<String> petitionSections = dossier != null ? textSupport.safeList(dossier.petitionSections()) : List.of();
        List<String> settlementLevers = dossier != null ? textSupport.safeList(dossier.settlementLevers()) : List.of();
        List<String> protocolChecklist = dossier != null ? textSupport.safeList(dossier.protocolChecklist()) : List.of();
        List<String> signals = textSupport.safeList(input.externalSignals());

        int gapCount = proofGaps.size();
        int anchorCount = evidenceAnchors.size();
        int checklistCount = protocolChecklist.size();
        int evidenceScore = scoringPolicy.normalizeScore(input.evidenceScore(), dossier != null ? dossier.evidentiaryBracket() : null, anchorCount, gapCount);
        int negotiationScore = scoringPolicy.normalizeScore(input.negotiationScore(), dossier != null ? dossier.negotiationBracket() : null, settlementLevers.size(), gapCount);
        int readinessScore = scoringPolicy.normalizeReadiness(input.readinessScore(), gapCount, anchorCount, checklistCount, input.urgent(), input.authorId(), input.counterpartyId());

        LinkedHashSet<String> pleadingBlueprint = buildPleadingBlueprint(input, petitionSections, controversyAxes, thesisVectors);
        LinkedHashSet<String> evidenceAgenda = buildEvidenceAgenda(input, evidenceAnchors, proofGaps, evidenceScore);
        LinkedHashSet<String> protocolBlockers = buildProtocolBlockers(input, gapCount, evidenceScore);
        LinkedHashSet<String> negotiationGuardrails = buildNegotiationGuardrails(input, settlementLevers, gapCount, negotiationScore);
        LinkedHashSet<String> executionChecklist = buildExecutionChecklist(input, protocolChecklist);
        LinkedHashSet<String> controlPoints = controlPointFactory.create(input, signals, controversyAxes, thesisVectors, gapCount, evidenceScore, negotiationScore, readinessScore, protocolBlockers.size());

        return new ProcessMaterialStrategyReport(
                scoringPolicy.classifyLitigationPosture(evidenceScore, negotiationScore, gapCount, input.urgent()),
                scoringPolicy.classifyProtocolReadiness(readinessScore, protocolBlockers.size()),
                scoringPolicy.classifyNegotiationStance(negotiationScore, gapCount, input.urgent()),
                scoringPolicy.classifyEvidenceReadiness(evidenceScore, gapCount),
                List.copyOf(pleadingBlueprint),
                List.copyOf(evidenceAgenda),
                List.copyOf(protocolBlockers),
                List.copyOf(negotiationGuardrails),
                List.copyOf(executionChecklist),
                List.copyOf(controlPoints),
                safeMap(metricsFactory.create(input, evidenceScore, negotiationScore, readinessScore, gapCount, anchorCount, checklistCount, signals.size(), controversyAxes.size(), thesisVectors.size()))
        );
    }


    private Map<String, Object> safeMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> safe = new java.util.LinkedHashMap<>(values);
        safe.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return java.util.Collections.unmodifiableMap(safe);
    }
    private LinkedHashSet<String> buildPleadingBlueprint(ProcessMaterialStrategyInput input,
                                                         List<String> petitionSections,
                                                         List<String> controversyAxes,
                                                         List<String> thesisVectors) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(ProcessMaterialStrategyMessages.pleadingObjectOpening());
        out.add(ProcessMaterialStrategyMessages.pleadingControversyAlignment());
        out.addAll(petitionSections);
        controversyAxes.stream().limit(4).forEach(axis -> out.add(ProcessMaterialStrategyMessages.controversyAxis(textSupport.compact(axis, 190))));
        thesisVectors.stream().limit(4).forEach(vector -> out.add(ProcessMaterialStrategyMessages.thesisVector(textSupport.compact(vector, 190))));
        if (input.urgent()) {
            out.add(ProcessMaterialStrategyMessages.urgentBlueprint());
        }
        if (input.juizado()) {
            out.add(ProcessMaterialStrategyMessages.juizadoBlueprint());
        }
        if (textSupport.containsAny(input.ritoName(), "EXECU", "CUMPRIMENTO", "MONITORIA")) {
            out.add(ProcessMaterialStrategyMessages.executionRiteBlueprint());
        }
        return out;
    }

    private LinkedHashSet<String> buildEvidenceAgenda(ProcessMaterialStrategyInput input,
                                                      List<String> evidenceAnchors,
                                                      List<String> proofGaps,
                                                      int evidenceScore) {
        LinkedHashSet<String> out = new LinkedHashSet<>(evidenceAnchors);
        if (out.isEmpty()) {
            out.add(ProcessMaterialStrategyMessages.missingEvidenceInventory());
        }
        proofGaps.stream().limit(5).forEach(gap -> out.add(ProcessMaterialStrategyMessages.proofGapSanitation(textSupport.compact(gap, 190))));
        if (textSupport.blank(input.authorId())) {
            out.add(ProcessMaterialStrategyMessages.missingAuthorIdentity());
        }
        if (textSupport.blank(input.counterpartyId())) {
            out.add(ProcessMaterialStrategyMessages.missingCounterpartyIdentity());
        }
        if (input.urgent() && evidenceScore < 75) {
            out.add(ProcessMaterialStrategyMessages.urgentEvidenceReinforcement());
        }
        return out;
    }

    private LinkedHashSet<String> buildProtocolBlockers(ProcessMaterialStrategyInput input,
                                                        int gapCount,
                                                        int evidenceScore) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (gapCount >= 4 || evidenceScore < 45) {
            out.add(ProcessMaterialStrategyMessages.weakEvidenceBlocker());
        }
        if (textSupport.blank(input.ritoName())) {
            out.add(ProcessMaterialStrategyMessages.missingRiteBlocker());
        }
        if (textSupport.blank(input.ramoDireito())) {
            out.add(ProcessMaterialStrategyMessages.missingBranchBlocker());
        }
        if (input.valorCausa() == null || input.valorCausa().compareTo(BigDecimal.ZERO) <= 0) {
            out.add(ProcessMaterialStrategyMessages.invalidCauseValueBlocker());
        }
        if (input.urgent() && evidenceScore < 60) {
            out.add(ProcessMaterialStrategyMessages.urgentWeakEvidenceBlocker());
        }
        if (input.juizado() && input.valorCausa() != null && input.valorCausa().compareTo(BigDecimal.ZERO) > 0) {
            out.addAll(checkJuizadoEconomicGuardrail(input.valorCausa()));
        }
        return out;
    }

    private List<String> checkJuizadoEconomicGuardrail(BigDecimal valorCausa) {
        if (valorCausa == null || valorCausa.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        BigDecimal salarios = valorCausa.divide(BigDecimal.valueOf(1621L), 4, RoundingMode.HALF_UP);
        return salarios.compareTo(BigDecimal.valueOf(40L)) > 0
                ? List.of(ProcessMaterialStrategyMessages.juizadoEconomicCeilingBlocker())
                : List.of();
    }

    private LinkedHashSet<String> buildNegotiationGuardrails(ProcessMaterialStrategyInput input,
                                                             List<String> settlementLevers,
                                                             int gapCount,
                                                             int negotiationScore) {
        LinkedHashSet<String> out = new LinkedHashSet<>(settlementLevers);
        out.add(ProcessMaterialStrategyMessages.negotiationObjectGuardrail());
        if (gapCount > 0) {
            out.add(ProcessMaterialStrategyMessages.negotiationGapGuardrail());
        }
        if (input.urgent()) {
            out.add(ProcessMaterialStrategyMessages.urgentNegotiationGuardrail());
        }
        if (input.juizado()) {
            out.add(ProcessMaterialStrategyMessages.juizadoNegotiationGuardrail());
        }
        if (negotiationScore < 55) {
            out.add(ProcessMaterialStrategyMessages.cautiousNegotiationGuardrail());
        }
        return out;
    }

    private LinkedHashSet<String> buildExecutionChecklist(ProcessMaterialStrategyInput input,
                                                          List<String> protocolChecklist) {
        LinkedHashSet<String> out = new LinkedHashSet<>(protocolChecklist);
        out.add(ProcessMaterialStrategyMessages.executionConsistencyCheck());
        out.add(ProcessMaterialStrategyMessages.executionEvidenceCheck());
        out.add(ProcessMaterialStrategyMessages.executionReliefCheck());
        if (input.valorCausa() != null && input.valorCausa().compareTo(BigDecimal.ZERO) > 0) {
            out.add(ProcessMaterialStrategyMessages.executionCauseValue(input.valorCausa()));
        }
        if (input.urgent()) {
            out.add(ProcessMaterialStrategyMessages.executionUrgencyCheck());
        }
        return out;
    }

}
