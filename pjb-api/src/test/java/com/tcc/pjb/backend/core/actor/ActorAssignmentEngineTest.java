package com.tcc.pjb.backend.core.actor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class ActorAssignmentEngineTest {

    private final PjbExecutionOrchestrator orchestrator = mock(PjbExecutionOrchestrator.class);
    private final ActorAssignmentEngine engine = new ActorAssignmentEngine(orchestrator);

    @Test
    void criticalExternalRitosMustGenerateActorCoverageWithoutGaps() {
        List<RitoProcessual> critical = List.of(
                RitoProcessual.COMUM_ORDINARIO,
                RitoProcessual.TRABALHISTA_ORDINARIO,
                RitoProcessual.ELEITORAL,
                RitoProcessual.MILITAR,
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL
        );

        when(orchestrator.supply(any(), any())).thenAnswer(invocation -> CompletableFuture.completedFuture(((java.util.function.Supplier<?>) invocation.getArgument(1)).get()));

        for (RitoProcessual rito : critical) {
            var snapshot = ProceduralCatalogSupport.snapshot(rito);
            FaseProcessual fase = FaseProcessual.valueOf(snapshot.stages().getFirst().getFase());
            var ctx = new ActorAssignmentEngine.AssignmentContext(
                    1L,
                    rito,
                    fase,
                    2,
                    true,
                    false,
                    false,
                    false,
                    fase == FaseProcessual.RECURSAL,
                    false,
                    List.of(),
                    Map.of()
            );

            var result = engine.assign(ctx);
            var gaps = engine.diagnoseExternalCoverage(rito, result.workItems());

            assertFalse(result.workItems().isEmpty(), rito.name());
            assertTrue(gaps.isEmpty(), rito.name() + " -> " + gaps);
        }
    }
    @Test
    void assignBatchMustDegradeOnAsyncTimeout() {
        PjbExecutionOrchestrator timeoutOrchestrator = mock(PjbExecutionOrchestrator.class);
        when(timeoutOrchestrator.supply(any(), any())).thenReturn(new CompletableFuture<>());
        ActorAssignmentEngine timeoutEngine = new ActorAssignmentEngine(timeoutOrchestrator);
        var ctx = ActorAssignmentEngine.AssignmentContext.of(99L, RitoProcessual.COMUM_ORDINARIO, FaseProcessual.CONHECIMENTO);

        var result = timeoutEngine.assignBatch(List.of(ctx));

        assertFalse(result.isEmpty());
        assertTrue(result.getFirst().diagnostics().stream().anyMatch(d -> d.contains("timeout controlado")));
    }

}