package com.tcc.pjb.backend.core.kernel.twin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.model.entity.Processo;
import org.junit.jupiter.api.Test;

class ProcessTwinProceduralOrchestratorTest {

    private final NationalProceduralRoutingService routingService = mock(NationalProceduralRoutingService.class);
    private final ProceduralSubmissionBlueprintService blueprintService = mock(ProceduralSubmissionBlueprintService.class);
    private final ProceduralConnectorExecutionService connectorService = mock(ProceduralConnectorExecutionService.class);
    private final ProcessTwinProceduralOrchestrator orchestrator = new ProcessTwinProceduralOrchestrator(routingService, blueprintService, connectorService);

    @Test
    void encadeiaAs3AnalisesPassandoORoutingParaBlueprintERoutingBlueprintParaConnector() {
        Processo processo = Processo.builder().id(1L).build();
        var routing = mock(ProceduralRoutingReport.class);
        var blueprint = mock(ProceduralSubmissionBlueprintReport.class);
        var connector = mock(ProceduralConnectorExecutionReport.class);
        when(routingService.analyzeProcess(processo)).thenReturn(routing);
        when(blueprintService.analyzeProcess(processo, routing)).thenReturn(blueprint);
        when(connectorService.analyzeProcess(processo, routing, blueprint)).thenReturn(connector);

        var bundle = orchestrator.analyzeProcess(processo);

        assertThat(bundle.routing()).isSameAs(routing);
        assertThat(bundle.submissionBlueprint()).isSameAs(blueprint);
        assertThat(bundle.connectorExecution()).isSameAs(connector);
        verify(routingService).analyzeProcess(processo);
        verify(blueprintService).analyzeProcess(processo, routing);
        verify(connectorService).analyzeProcess(processo, routing, blueprint);
    }
}
