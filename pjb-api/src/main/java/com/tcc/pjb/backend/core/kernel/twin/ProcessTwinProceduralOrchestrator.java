package com.tcc.pjb.backend.core.kernel.twin;

import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.model.entity.Processo;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de ProcessDigitalTwinService: cadeia procedimental de análise de processo
 * (roteamento nacional -> blueprint de submissão -> execução de conector externo).
 * Os 3 relatórios são calculados em sequência porque cada um depende do anterior; o bundle
 * devolve os 3 juntos porque o twin() usa todos.
 */
@Service
public class ProcessTwinProceduralOrchestrator {

    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService;

    public ProcessTwinProceduralOrchestrator(NationalProceduralRoutingService nationalProceduralRoutingService,
                                              ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService,
                                              ProceduralConnectorExecutionService proceduralConnectorExecutionService) {
        this.nationalProceduralRoutingService = Objects.requireNonNull(nationalProceduralRoutingService);
        this.proceduralSubmissionBlueprintService = Objects.requireNonNull(proceduralSubmissionBlueprintService);
        this.proceduralConnectorExecutionService = Objects.requireNonNull(proceduralConnectorExecutionService);
    }

    public Bundle analyzeProcess(Processo processo) {
        ProceduralRoutingReport routing = nationalProceduralRoutingService.analyzeProcess(processo);
        ProceduralSubmissionBlueprintReport blueprint = proceduralSubmissionBlueprintService.analyzeProcess(processo, routing);
        ProceduralConnectorExecutionReport connector = proceduralConnectorExecutionService.analyzeProcess(processo, routing, blueprint);
        return new Bundle(routing, blueprint, connector);
    }

    public record Bundle(
            ProceduralRoutingReport routing,
            ProceduralSubmissionBlueprintReport submissionBlueprint,
            ProceduralConnectorExecutionReport connectorExecution
    ) {
    }
}
