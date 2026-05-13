package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessMaterialStrategyService {

    private final ProcessMaterialStrategyInputFactory inputFactory;
    private final ProcessMaterialStrategyReportFactory reportFactory;

    public ProcessMaterialStrategyService() {
        ProcessMaterialStrategyTextSupport textSupport = new ProcessMaterialStrategyTextSupport();
        ProcessMaterialStrategyScoringPolicy scoringPolicy = new ProcessMaterialStrategyScoringPolicy(textSupport);
        ProcessMaterialStrategyMetricsFactory metricsFactory = new ProcessMaterialStrategyMetricsFactory();
        ProcessMaterialStrategyControlPointFactory controlPointFactory = new ProcessMaterialStrategyControlPointFactory(textSupport, scoringPolicy);
        this.inputFactory = new ProcessMaterialStrategyInputFactory(textSupport, scoringPolicy);
        this.reportFactory = new ProcessMaterialStrategyReportFactory(textSupport, scoringPolicy, metricsFactory, controlPointFactory);
    }

    ProcessMaterialStrategyService(ProcessMaterialStrategyInputFactory inputFactory,
                                   ProcessMaterialStrategyReportFactory reportFactory) {
        this.inputFactory = Objects.requireNonNull(inputFactory);
        this.reportFactory = Objects.requireNonNull(reportFactory);
    }

    public ProcessMaterialStrategyReport analyzeProcess(Processo processo,
                                                        ProcessMaterialDossierReport dossier,
                                                        List<String> externalSignals) {
        return reportFactory.create(inputFactory.fromProcess(processo, dossier, externalSignals));
    }

    public ProcessMaterialStrategyReport analyzeRequest(LaianePeticaoAssistRequest request,
                                                        CanonicalContext canonical,
                                                        String ritoName,
                                                        ProcessMaterialDossierReport dossier,
                                                        double readinessScore,
                                                        List<String> externalSignals) {
        return reportFactory.create(inputFactory.fromRequest(request, canonical, ritoName, dossier, readinessScore, externalSignals));
    }
}
