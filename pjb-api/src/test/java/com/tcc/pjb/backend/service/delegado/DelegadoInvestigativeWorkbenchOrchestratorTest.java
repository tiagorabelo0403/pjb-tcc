package com.tcc.pjb.backend.service.delegado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeExecutionService;
import com.tcc.pjb.backend.service.criminal.PjbPoliceNativeToolbeltService;
import com.tcc.pjb.backend.service.criminal.PoliceInvestigationSystemLandscapeService;
import com.tcc.pjb.backend.service.criminal.PoliceSovereignOperationalWorkbenchService;
import com.tcc.pjb.backend.service.criminal.PoliceTraceableExecutionLedgerService;
import com.tcc.pjb.backend.service.criminal.PoliceTransactionalAdapterMeshService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DelegadoInvestigativeWorkbenchOrchestratorTest {

    private final PoliceInvestigationSystemLandscapeService landscapeService = mock(PoliceInvestigationSystemLandscapeService.class);
    private final PjbPoliceNativeToolbeltService nativeToolbeltService = mock(PjbPoliceNativeToolbeltService.class);
    private final PoliceTransactionalAdapterMeshService adapterMeshService = mock(PoliceTransactionalAdapterMeshService.class);
    private final PoliceSovereignOperationalWorkbenchService sovereignWorkbenchService = mock(PoliceSovereignOperationalWorkbenchService.class);
    private final PjbPoliceNativeExecutionService nativeExecutionService = mock(PjbPoliceNativeExecutionService.class);
    private final PoliceTraceableExecutionLedgerService ledgerService = mock(PoliceTraceableExecutionLedgerService.class);
    private final DelegadoInvestigativeWorkbenchOrchestrator orchestrator = new DelegadoInvestigativeWorkbenchOrchestrator(
            landscapeService, nativeToolbeltService, adapterMeshService, sovereignWorkbenchService, nativeExecutionService, ledgerService);

    @Test
    void landscapeForDelegaComTipoUsuario() {
        Map<String, Object> response = Map.of("landscape", "police-fed");
        when(landscapeService.landscapeFor(TipoUsuario.DELEGADO_POLICIA)).thenReturn(response);

        assertThat(orchestrator.landscapeFor(TipoUsuario.DELEGADO_POLICIA)).isSameAs(response);
    }

    @Test
    void composeInvestigativeWorkstationEmpilha5BlocosNaOrdemOriginalDoBootstrapPainel() {
        TipoUsuario tipo = TipoUsuario.DELEGADO_POLICIA;
        Map<String, Object> baseSoverana = new LinkedHashMap<>();
        baseSoverana.put("baseKey", "baseValue");
        Map<String, Object> toolbelt = Map.of("t", 1);
        Map<String, Object> mesh = Map.of("m", 2);
        Map<String, Object> execution = Map.of("e", 3);
        Map<String, Object> ledgerBlueprint = Map.of("l", 4);
        Map<String, Object> recentExecutions = Map.of("r", 5);
        when(sovereignWorkbenchService.compose(tipo)).thenReturn(baseSoverana);
        when(nativeToolbeltService.nativeWorkbench(tipo)).thenReturn(toolbelt);
        when(adapterMeshService.sovereignMesh(tipo)).thenReturn(mesh);
        when(nativeExecutionService.nativeExecutionWorkbench(tipo)).thenReturn(execution);
        when(ledgerService.operationalLedgerBlueprint(tipo)).thenReturn(ledgerBlueprint);
        when(ledgerService.recentExecutions(tipo, 8)).thenReturn(recentExecutions);

        Map<String, Object> result = orchestrator.composeInvestigativeWorkstation(tipo);

        assertThat(result)
                .containsEntry("baseKey", "baseValue")
                .containsEntry("nativeToolbelt", toolbelt)
                .containsEntry("transactionalAdapterMesh", mesh)
                .containsEntry("nativeExecutionWorkbench", execution)
                .containsEntry("traceableOperationalLedger", ledgerBlueprint)
                .containsEntry("recentTraceableExecutions", recentExecutions);
        verify(ledgerService).recentExecutions(tipo, 8);
    }

    @Test
    void composeInvestigativeWorkstationSempreRetornaMapMutavel() {
        TipoUsuario tipo = TipoUsuario.DELEGADO_POLICIA_FEDERAL;
        when(sovereignWorkbenchService.compose(tipo)).thenReturn(Map.of("imutavel", 1));
        when(nativeToolbeltService.nativeWorkbench(tipo)).thenReturn(Map.of());
        when(adapterMeshService.sovereignMesh(tipo)).thenReturn(Map.of());
        when(nativeExecutionService.nativeExecutionWorkbench(tipo)).thenReturn(Map.of());
        when(ledgerService.operationalLedgerBlueprint(tipo)).thenReturn(Map.of());
        when(ledgerService.recentExecutions(tipo, 8)).thenReturn(Map.of());

        Map<String, Object> result = orchestrator.composeInvestigativeWorkstation(tipo);

        // Precisa ser mutavel porque a pipeline de composicao do painel adiciona chaves depois
        result.put("teste_mutavel", "ok");
        assertThat(result).containsEntry("teste_mutavel", "ok");
    }
}
