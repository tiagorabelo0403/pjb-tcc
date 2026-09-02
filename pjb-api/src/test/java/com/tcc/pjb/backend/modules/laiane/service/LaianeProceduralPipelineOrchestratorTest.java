package com.tcc.pjb.backend.modules.laiane.service;

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
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import org.junit.jupiter.api.Test;

class LaianeProceduralPipelineOrchestratorTest {

    private final NationalProceduralRoutingService routingService = mock(NationalProceduralRoutingService.class);
    private final TerritorialProcessualService territorialService = mock(TerritorialProcessualService.class);
    private final ProceduralSubmissionBlueprintService blueprintService = mock(ProceduralSubmissionBlueprintService.class);
    private final ProceduralConnectorExecutionService connectorService = mock(ProceduralConnectorExecutionService.class);
    private final LaianeProceduralPipelineOrchestrator orchestrator = new LaianeProceduralPipelineOrchestrator(
            routingService, territorialService, blueprintService, connectorService);

    @Test
    void encadeiaRoutingTerritorialBlueprintConnectorNaOrdemCorreta() {
        var request = mock(LaianePeticaoAssistRequest.class);
        Processo processo = Processo.builder().id(1L).build();
        var routing = mock(ProceduralRoutingReport.class);
        var territorial = mock(TerritorialProcessualService.DiagnosticoTerritorialProcessual.class);
        var blueprint = mock(ProceduralSubmissionBlueprintReport.class);
        var connector = mock(ProceduralConnectorExecutionReport.class);
        when(routingService.analyzeRequest(request)).thenReturn(routing);
        when(territorialService.diagnosticar(processo, routing)).thenReturn(territorial);
        when(blueprintService.analyzeProcess(processo, routing)).thenReturn(blueprint);
        when(connectorService.analyzeProcess(processo, routing, blueprint)).thenReturn(connector);

        var bundle = orchestrator.analyze(request, processo);

        assertThat(bundle.routing()).isSameAs(routing);
        assertThat(bundle.territorial()).isSameAs(territorial);
        assertThat(bundle.submissionBlueprint()).isSameAs(blueprint);
        assertThat(bundle.connectorExecution()).isSameAs(connector);
        verify(routingService).analyzeRequest(request);
        verify(territorialService).diagnosticar(processo, routing);
        verify(blueprintService).analyzeProcess(processo, routing);
        verify(connectorService).analyzeProcess(processo, routing, blueprint);
    }
}
