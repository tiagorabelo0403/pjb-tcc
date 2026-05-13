package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.util.List;
import java.util.Objects;

final class StrategicCopilotRequestReportFactory {

    private final StrategicCopilotSupport support;
    private final StrategicCopilotDiagnosticsFactory diagnosticsFactory;

    StrategicCopilotRequestReportFactory(StrategicCopilotSupport support,
                                         StrategicCopilotDiagnosticsFactory diagnosticsFactory) {
        this.support = Objects.requireNonNull(support);
        this.diagnosticsFactory = Objects.requireNonNull(diagnosticsFactory);
    }

    StrategicCopilotReport create(LaianePeticaoAssistRequest request,
                                  CanonicalContext canonical,
                                  String ritoName,
                                  LegalCoherenceReport coherence,
                                  ProtocolDryRunReport dryRun,
                                  ProcessIntegrityRadarReport radar,
                                  DynamicCompetenceDistributionResponse competencia) {
        Objects.requireNonNull(request, "request");
        StrategicCopilotDraft draft = support.petitionAssistDraft();

        applyCoherence(draft, coherence);
        applyDryRun(draft, dryRun);
        applyRadar(draft, radar);
        applyCompetence(draft, competencia);
        applyUrgentRelief(draft, request);
        applyJuizadoLane(draft, request);
        applyPrecedentLane(draft);
        applyWatchpoints(draft, request, canonical, ritoName);

        return draft.toReport(
                "PETITION_ASSIST",
                support.normalizePhaseLabel(null),
                diagnosticsFactory.petitionAssist(ritoName, canonical, coherence, dryRun, radar),
                support
        );
    }

    private void applyCoherence(StrategicCopilotDraft draft, LegalCoherenceReport coherence) {
        if (coherence != null && coherence.blocking()) {
            draft.immediate(support.action(
                    "BLOCKING_COHERENCE",
                    StrategicCopilotMessages.blockingCoherenceTitle(),
                    "CRITICAL",
                    StrategicCopilotMessages.blockingCoherenceRationale(),
                    coherence.strategicRecommendations().isEmpty()
                            ? StrategicCopilotMessages.blockingCoherenceFallbackSteps()
                            : coherence.strategicRecommendations()
            ));
            draft.score(-0.18d);
            return;
        }
        draft.score(0.08d);
    }

    private void applyDryRun(StrategicCopilotDraft draft, ProtocolDryRunReport dryRun) {
        if (dryRun != null && !dryRun.apto()) {
            draft.procedural(support.action(
                    "DRY_RUN_REVIEW",
                    StrategicCopilotMessages.dryRunReviewTitle(),
                    support.hasCritical(dryRun) ? "CRITICAL" : "HIGH",
                    StrategicCopilotMessages.dryRunReviewRationale(),
                    dryRun.nextActions()
            ));
            draft.score(support.hasCritical(dryRun) ? -0.14d : -0.08d);
            return;
        }
        if (dryRun != null) {
            draft.procedural(support.action(
                    "DRY_RUN_READY",
                    StrategicCopilotMessages.dryRunReadyTitle(),
                    "LOW",
                    StrategicCopilotMessages.dryRunReadyRationale(),
                    StrategicCopilotMessages.dryRunReadySteps()
            ));
            draft.score(0.06d);
        }
    }

    private void applyRadar(StrategicCopilotDraft draft, ProcessIntegrityRadarReport radar) {
        if (radar == null || radar.findings().isEmpty()) {
            return;
        }
        radar.findings().stream()
                .filter(f -> "EVIDENCE".equals(f.domain()) || "JURISPRUDENCE".equals(f.domain()))
                .findFirst()
                .ifPresent(f -> draft.evidence(support.action(
                        f.code(),
                        StrategicCopilotMessages.evidenceReinforcementTitle(),
                        f.severity(),
                        f.message(),
                        StrategicCopilotMessages.evidenceReinforcementSteps()
                )));
        radar.findings().stream()
                .filter(f -> "RECURSAL".equals(f.domain()) || "NULLITY".equals(f.domain()))
                .forEach(f -> draft.procedural(support.action(
                        f.code(),
                        f.title(),
                        f.severity(),
                        f.message(),
                        StrategicCopilotMessages.proceduralRadarSteps()
                )));
        draft.watchpoints(radar.watchpoints());
        draft.score(radar.blocking() ? -0.12d : -0.04d);
    }

    private void applyCompetence(StrategicCopilotDraft draft,
                                 DynamicCompetenceDistributionResponse competencia) {
        if (competencia == null || !competencia.distribuicaoAutomatica()) {
            draft.procedural(support.action(
                    "COMPETENCE_REVIEW",
                    StrategicCopilotMessages.competenceReviewTitle(),
                    competencia == null ? "CRITICAL" : "HIGH",
                    StrategicCopilotMessages.competenceReviewRationale(competencia, support),
                    StrategicCopilotMessages.competenceReviewSteps()
            ));
            draft.score(-0.10d);
            return;
        }
        draft.score(0.05d);
    }

    private void applyUrgentRelief(StrategicCopilotDraft draft, LaianePeticaoAssistRequest request) {
        if (!support.truthy(request.getRequerLiminar())) {
            return;
        }
        draft.immediate(support.action(
                "URGENT_RELIEF_STRATEGY",
                StrategicCopilotMessages.urgentReliefTitle(),
                support.blank(request.getTextoFatosResumido()) ? "HIGH" : "MEDIUM",
                StrategicCopilotMessages.urgentReliefRationale(),
                StrategicCopilotMessages.urgentReliefSteps()
        ));
    }

    private void applyJuizadoLane(StrategicCopilotDraft draft, LaianePeticaoAssistRequest request) {
        if (!support.truthy(request.getRequerJuizadoEspecial())) {
            return;
        }
        draft.negotiation(support.action(
                "JUÍZADO_SETTLEMENT_LANE",
                StrategicCopilotMessages.juizadoSettlementTitle(),
                "LOW",
                StrategicCopilotMessages.juizadoSettlementRationale(),
                StrategicCopilotMessages.juizadoSettlementSteps()
        ));
    }

    private void applyPrecedentLane(StrategicCopilotDraft draft) {
        draft.jurisprudential(support.action(
                "PRECEDENT_CURATION",
                StrategicCopilotMessages.precedentCurationTitle(),
                "MEDIUM",
                StrategicCopilotMessages.precedentCurationRationale(),
                StrategicCopilotMessages.precedentCurationSteps()
        ));
    }

    private void applyWatchpoints(StrategicCopilotDraft draft,
                                  LaianePeticaoAssistRequest request,
                                  CanonicalContext canonical,
                                  String ritoName) {
        if (!support.blank(ritoName)) {
            draft.watchpoint(StrategicCopilotMessages.watchpointRitoNominal(ritoName));
        }
        if (canonical != null && !support.blank(canonical.classeTpuCodigo())) {
            draft.watchpoint(StrategicCopilotMessages.watchpointClasseTpu(canonical.classeTpuCodigo()));
        }
        if (support.blank(request.getCpfCnpjAutor()) || support.blank(request.getCpfCnpjReu())) {
            draft.watchpoint(StrategicCopilotMessages.qualificationReviewWatchpoint());
            draft.score(-0.05d);
        }
    }
}
