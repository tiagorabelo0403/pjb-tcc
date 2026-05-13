package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessMaterialDossierService {

    private final ProcessMaterialDossierInputFactory inputFactory;
    private final ProcessMaterialDossierHeuristics heuristics;
    private final ProcessMaterialDossierDiagnosticsFactory diagnosticsFactory;

    public ProcessMaterialDossierService() {
        ProcessMaterialDossierTextSupport textSupport = new ProcessMaterialDossierTextSupport();
        this.inputFactory = new ProcessMaterialDossierInputFactory(textSupport);
        this.heuristics = new ProcessMaterialDossierHeuristics(textSupport);
        this.diagnosticsFactory = new ProcessMaterialDossierDiagnosticsFactory(textSupport);
    }

    ProcessMaterialDossierService(ProcessMaterialDossierInputFactory inputFactory,
                                  ProcessMaterialDossierHeuristics heuristics,
                                  ProcessMaterialDossierDiagnosticsFactory diagnosticsFactory) {
        this.inputFactory = Objects.requireNonNull(inputFactory);
        this.heuristics = Objects.requireNonNull(heuristics);
        this.diagnosticsFactory = Objects.requireNonNull(diagnosticsFactory);
    }

    public ProcessMaterialDossierReport analyzeProcess(Processo processo, List<String> riskSignals) {
        return analyze(inputFactory.fromProcess(processo, riskSignals));
    }

    public ProcessMaterialDossierReport analyzeRequest(LaianePeticaoAssistRequest request,
                                                       CanonicalContext canonical,
                                                       String ritoName) {
        return analyze(inputFactory.fromRequest(request, canonical, ritoName));
    }

    private ProcessMaterialDossierReport analyze(ProcessMaterialDossierInput input) {
        ProcessMaterialDossierAnalysis analysis = heuristics.analyze(input);
        Map<String, Object> diagnostics = diagnosticsFactory.create(input, analysis);
        return new ProcessMaterialDossierReport(
                input.lane(),
                input.phase(),
                analysis.objectLabel(),
                analysis.primaryRelief(),
                analysis.evidentiaryBracket(),
                analysis.negotiationBracket(),
                analysis.controversyAxes(),
                analysis.thesisVectors(),
                analysis.evidenceAnchors(),
                analysis.proofGaps(),
                analysis.petitionSections(),
                analysis.settlementLevers(),
                analysis.protocolChecklist(),
                diagnostics
        );
    }
}
