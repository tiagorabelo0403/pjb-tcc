package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;
import java.util.Map;
import java.util.Objects;

final class StrategicCopilotDiagnosticsFactory {

    private final StrategicCopilotSupport support;

    StrategicCopilotDiagnosticsFactory(StrategicCopilotSupport support) {
        this.support = Objects.requireNonNull(support);
    }

    Map<String, Object> petitionAssist(String ritoName,
                                       CanonicalContext canonical,
                                       LegalCoherenceReport coherence,
                                       ProtocolDryRunReport dryRun,
                                       ProcessIntegrityRadarReport radar) {
        return PayloadMaps.ofEntries(
                "lane", "PETITION_ASSIST",
                "phase", support.normalizePhaseLabel(null),
                "ritoName", ritoName,
                "classeTpu", canonical != null ? canonical.classeTpuCodigo() : null,
                "blocking", coherence != null && coherence.blocking(),
                "dryRunStatus", dryRun != null ? dryRun.status() : null,
                "integrityStatus", radar != null ? radar.status() : null
        );
    }

    Map<String, Object> processTwin(FaseProcessual fase,
                                    String ritoName,
                                    RitoPlanDto ritoPlan,
                                    ProcessIntegrityRadarReport radar,
                                    SettlementAdvisoryReport settlement) {
        return PayloadMaps.ofEntries(
                "lane", "PROCESS_TWIN",
                "phase", support.normalizePhaseLabel(fase),
                "ritoName", ritoName,
                "workflowBlockingOpen", ritoPlan != null && ritoPlan.getBlockingOpen() != null ? ritoPlan.getBlockingOpen().size() : 0,
                "integrityStatus", radar != null ? radar.status() : null,
                "settlementStatus", settlement != null ? settlement.status() : null
        );
    }
}
