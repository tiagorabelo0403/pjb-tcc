package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;
import java.util.Objects;

final class StrategicCopilotProcessReportFactory {

    private final StrategicCopilotSupport support;
    private final StrategicCopilotDiagnosticsFactory diagnosticsFactory;

    StrategicCopilotProcessReportFactory(StrategicCopilotSupport support,
                                         StrategicCopilotDiagnosticsFactory diagnosticsFactory) {
        this.support = Objects.requireNonNull(support);
        this.diagnosticsFactory = Objects.requireNonNull(diagnosticsFactory);
    }

    StrategicCopilotReport create(Processo processo,
                                  String ritoName,
                                  RitoPlanDto ritoPlan,
                                  LegalCoherenceReport coherence,
                                  ProtocolDryRunReport dryRun,
                                  ProcessIntegrityRadarReport radar,
                                  SettlementAdvisoryReport settlement) {
        Objects.requireNonNull(processo, "processo");
        FaseProcessual fase = processo.getFaseAtual();
        StrategicCopilotDraft draft = support.processTwinDraft();

        applyWorkflowBlockers(draft, ritoPlan);
        applyRadarBlocking(draft, radar);
        applyPhaseLane(draft, fase);
        applyCoherence(draft, coherence);
        applySettlement(draft, settlement);
        applyWatchpoints(draft, ritoName, dryRun);

        return draft.toReport(
                "PROCESS_TWIN",
                support.normalizePhaseLabel(fase),
                diagnosticsFactory.processTwin(fase, ritoName, ritoPlan, radar, settlement),
                support
        );
    }

    private void applyWorkflowBlockers(StrategicCopilotDraft draft, RitoPlanDto ritoPlan) {
        if (ritoPlan == null || ritoPlan.getBlockingOpen() == null || ritoPlan.getBlockingOpen().isEmpty()) {
            return;
        }
        draft.procedural(support.action(
                "WORKFLOW_BLOCKERS",
                StrategicCopilotMessages.workflowBlockersTitle(),
                "HIGH",
                StrategicCopilotMessages.workflowBlockersRationale(),
                StrategicCopilotMessages.workflowBlockersSteps()
        ));
        draft.score(-0.11d);
    }

    private void applyRadarBlocking(StrategicCopilotDraft draft, ProcessIntegrityRadarReport radar) {
        if (radar == null || !radar.blocking()) {
            return;
        }
        draft.immediate(support.action(
                "INTEGRITY_BLOCKING",
                StrategicCopilotMessages.integrityBlockingTitle(),
                "CRITICAL",
                StrategicCopilotMessages.integrityBlockingRationale(),
                radar.nextActions()
        ));
        draft.score(-0.16d);
    }

    private void applyPhaseLane(StrategicCopilotDraft draft, FaseProcessual fase) {
        switch (fase == null ? FaseProcessual.CONHECIMENTO : fase) {
            case CONHECIMENTO, COGNITIVA, INSTRUTORIA -> applyCognitiveLane(draft);
            case RECURSAL -> applyRecursalLane(draft);
            case EXECUCAO, CUMPRIMENTO_SENTENCA, EXECUTORIA, PENHORA -> applyExecutionLane(draft);
            default -> {
            }
        }
    }

    private void applyCognitiveLane(StrategicCopilotDraft draft) {
        draft.evidence(support.action(
                "PROOF_CONSOLIDATION",
                StrategicCopilotMessages.proofConsolidationTitle(),
                "MEDIUM",
                StrategicCopilotMessages.proofConsolidationRationale(),
                StrategicCopilotMessages.proofConsolidationSteps()
        ));
        draft.jurisprudential(support.action(
                "MERITS_PRECEDENTS",
                StrategicCopilotMessages.meritsPrecedentsTitle(),
                "MEDIUM",
                StrategicCopilotMessages.meritsPrecedentsRationale(),
                StrategicCopilotMessages.meritsPrecedentsSteps()
        ));
    }

    private void applyRecursalLane(StrategicCopilotDraft draft) {
        draft.immediate(support.action(
                "RECURSAL_FOCUS",
                StrategicCopilotMessages.recursalFocusTitle(),
                "HIGH",
                StrategicCopilotMessages.recursalFocusRationale(),
                StrategicCopilotMessages.recursalFocusSteps()
        ));
        draft.jurisprudential(support.action(
                "AD_QUEM_PRECEDENTS",
                StrategicCopilotMessages.adQuemPrecedentsTitle(),
                "MEDIUM",
                StrategicCopilotMessages.adQuemPrecedentsRationale(),
                StrategicCopilotMessages.adQuemPrecedentsSteps()
        ));
        draft.score(-0.03d);
    }

    private void applyExecutionLane(StrategicCopilotDraft draft) {
        draft.immediate(support.action(
                "EXECUTION_EFFICIENCY",
                StrategicCopilotMessages.executionEfficiencyTitle(),
                "MEDIUM",
                StrategicCopilotMessages.executionEfficiencyRationale(),
                StrategicCopilotMessages.executionEfficiencySteps()
        ));
        draft.negotiation(support.action(
                "EXECUTION_SETTLEMENT",
                StrategicCopilotMessages.executionSettlementTitle(),
                "LOW",
                StrategicCopilotMessages.executionSettlementRationale(),
                StrategicCopilotMessages.executionSettlementSteps()
        ));
        draft.score(0.04d);
    }

    private void applyCoherence(StrategicCopilotDraft draft, LegalCoherenceReport coherence) {
        if (coherence == null || !coherence.blocking()) {
            return;
        }
        draft.procedural(support.action(
                "COHERENCE_REPAIR",
                StrategicCopilotMessages.coherenceRepairTitle(),
                "CRITICAL",
                StrategicCopilotMessages.coherenceRepairRationale(),
                coherence.strategicRecommendations()
        ));
        draft.score(-0.12d);
    }

    private void applySettlement(StrategicCopilotDraft draft, SettlementAdvisoryReport settlement) {
        if (settlement == null) {
            return;
        }
        draft.negotiation(support.action(
                "SETTLEMENT_LANE",
                StrategicCopilotMessages.settlementLaneTitle(),
                settlement.executable() ? "LOW" : "HIGH",
                StrategicCopilotMessages.settlementLaneRationale(settlement.executable()),
                settlement.nextMoves()
        ));
        draft.watchpoints(settlement.executionSafeguards());
        draft.score(settlement.executable() ? 0.05d : -0.05d);
    }

    private void applyWatchpoints(StrategicCopilotDraft draft,
                                  String ritoName,
                                  ProtocolDryRunReport dryRun) {
        if (!support.blank(ritoName)) {
            draft.watchpoint(StrategicCopilotMessages.watchpointRitoEfetivo(ritoName));
        }
        if (dryRun != null && !dryRun.apto()) {
            draft.watchpoints(dryRun.nextActions());
        }
    }
}
