package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.service.rito.dto.RitoPlanDto;
import java.util.Objects;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class StrategicCopilotService {

    private final StrategicCopilotRequestReportFactory requestReportFactory;
    private final StrategicCopilotProcessReportFactory processReportFactory;

    @Inject
    @Autowired
    public StrategicCopilotService() {
        StrategicCopilotSupport support = new StrategicCopilotSupport();
        StrategicCopilotDiagnosticsFactory diagnosticsFactory = new StrategicCopilotDiagnosticsFactory(support);
        this.requestReportFactory = new StrategicCopilotRequestReportFactory(support, diagnosticsFactory);
        this.processReportFactory = new StrategicCopilotProcessReportFactory(support, diagnosticsFactory);
    }

    StrategicCopilotService(StrategicCopilotRequestReportFactory requestReportFactory,
                            StrategicCopilotProcessReportFactory processReportFactory) {
        this.requestReportFactory = Objects.requireNonNull(requestReportFactory);
        this.processReportFactory = Objects.requireNonNull(processReportFactory);
    }

    public StrategicCopilotReport analyzeRequest(LaianePeticaoAssistRequest request,
                                                 CanonicalContext canonical,
                                                 String ritoName,
                                                 LegalCoherenceReport coherence,
                                                 ProtocolDryRunReport dryRun,
                                                 ProcessIntegrityRadarReport radar,
                                                 DynamicCompetenceDistributionResponse competencia) {
        return requestReportFactory.create(request, canonical, ritoName, coherence, dryRun, radar, competencia);
    }

    public StrategicCopilotReport analyzeProcess(Processo processo,
                                                 String ritoName,
                                                 RitoPlanDto ritoPlan,
                                                 LegalCoherenceReport coherence,
                                                 ProtocolDryRunReport dryRun,
                                                 ProcessIntegrityRadarReport radar,
                                                 SettlementAdvisoryReport settlement) {
        return processReportFactory.create(processo, ritoName, ritoPlan, coherence, dryRun, radar, settlement);
    }
}
