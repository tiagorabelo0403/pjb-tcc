package com.tcc.pjb.backend.modules.laiane.service;

import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de LaianePeticaoAssistService: pipeline procedimental para request de
 * petição -- roteamento nacional (request), diagnóstico territorial + blueprint de
 * submissão + execução de conector (todos sobre o Processo sintético). Encadeamento
 * routing -> territorial (com routing) -> blueprint (com routing) -> connector (com
 * routing+blueprint) preservado byte-a-byte da versão original.
 */
@Service
public class LaianeProceduralPipelineOrchestrator {

    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final TerritorialProcessualService territorialProcessualService;
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService;

    public LaianeProceduralPipelineOrchestrator(NationalProceduralRoutingService nationalProceduralRoutingService,
                                                 TerritorialProcessualService territorialProcessualService,
                                                 ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService,
                                                 ProceduralConnectorExecutionService proceduralConnectorExecutionService) {
        this.nationalProceduralRoutingService = Objects.requireNonNull(nationalProceduralRoutingService);
        this.territorialProcessualService = Objects.requireNonNull(territorialProcessualService);
        this.proceduralSubmissionBlueprintService = Objects.requireNonNull(proceduralSubmissionBlueprintService);
        this.proceduralConnectorExecutionService = Objects.requireNonNull(proceduralConnectorExecutionService);
    }

    public Bundle analyze(LaianePeticaoAssistRequest request, Processo syntheticProcess) {
        ProceduralRoutingReport routing = nationalProceduralRoutingService.analyzeRequest(request);
        TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial =
                territorialProcessualService.diagnosticar(syntheticProcess, routing);
        ProceduralSubmissionBlueprintReport submissionBlueprint =
                proceduralSubmissionBlueprintService.analyzeProcess(syntheticProcess, routing);
        ProceduralConnectorExecutionReport connectorExecution =
                proceduralConnectorExecutionService.analyzeProcess(syntheticProcess, routing, submissionBlueprint);
        return new Bundle(routing, territorial, submissionBlueprint, connectorExecution);
    }

    public record Bundle(
            ProceduralRoutingReport routing,
            TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
            ProceduralSubmissionBlueprintReport submissionBlueprint,
            ProceduralConnectorExecutionReport connectorExecution
    ) {
    }
}
