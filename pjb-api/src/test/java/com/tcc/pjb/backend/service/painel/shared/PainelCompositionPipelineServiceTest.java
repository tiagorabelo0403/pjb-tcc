package com.tcc.pjb.backend.service.painel.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PainelCompositionPipelineServiceTest {

    private final PainelSignalReflectionService signalReflectionService = mock(PainelSignalReflectionService.class);
    private final PainelNativeCollectionCompositionService collectionCompositionService = mock(PainelNativeCollectionCompositionService.class);
    private final PainelActionSurfaceCompositionService actionSurfaceCompositionService = mock(PainelActionSurfaceCompositionService.class);
    private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService = mock(PainelExecutionSurfaceCompositionService.class);
    private final PainelCompositionPipelineService pipeline = new PainelCompositionPipelineService(
            signalReflectionService, collectionCompositionService, actionSurfaceCompositionService, executionSurfaceCompositionService);

    @Test
    void decorateEncadeiaAs4CamadasNaOrdemCorretaThreadandoOResultado() {
        Map<String, Object> raw = Map.of("k", "v");
        Map<String, Object> signals = Map.of("s", 1);
        Map<String, Object> nativeComposition = Map.of("n", 2);
        Map<String, Object> actionSurface = Map.of("a", 3);
        Map<String, Object> executionSurface = Map.of("e", 4);
        Map<String, Object> afterReflect = Map.of("step", "reflect");
        Map<String, Object> afterCollection = Map.of("step", "collection");
        Map<String, Object> afterAction = Map.of("step", "action");
        Map<String, Object> afterExecution = Map.of("step", "execution");
        when(signalReflectionService.reflectInBlock("PANEL", "BLOCK", raw, signals)).thenReturn(afterReflect);
        when(collectionCompositionService.decorateBlock("PANEL", "BLOCK", afterReflect, signals, nativeComposition)).thenReturn(afterCollection);
        when(actionSurfaceCompositionService.decorateBlock("PANEL", "BLOCK", afterCollection, actionSurface, nativeComposition)).thenReturn(afterAction);
        when(executionSurfaceCompositionService.decorateBlock("PANEL", "BLOCK", afterAction, executionSurface, nativeComposition)).thenReturn(afterExecution);

        Map<String, Object> result = pipeline.decorate("PANEL", "BLOCK", raw, signals, nativeComposition, actionSurface, executionSurface);

        assertThat(result).isEqualTo(afterExecution);
    }

    @Test
    void decorateWithoutCollectionPulaACamadaDeColecaoNativa() {
        Map<String, Object> raw = Map.of("k", "v");
        Map<String, Object> signals = Map.of();
        Map<String, Object> nativeComposition = Map.of();
        Map<String, Object> actionSurface = Map.of();
        Map<String, Object> executionSurface = Map.of();
        Map<String, Object> afterReflect = Map.of("step", "reflect");
        Map<String, Object> afterAction = Map.of("step", "action");
        Map<String, Object> afterExecution = Map.of("step", "execution");
        when(signalReflectionService.reflectInBlock("PANEL", "BLOCK", raw, signals)).thenReturn(afterReflect);
        when(actionSurfaceCompositionService.decorateBlock("PANEL", "BLOCK", afterReflect, actionSurface, nativeComposition)).thenReturn(afterAction);
        when(executionSurfaceCompositionService.decorateBlock("PANEL", "BLOCK", afterAction, executionSurface, nativeComposition)).thenReturn(afterExecution);

        Map<String, Object> result = pipeline.decorateWithoutCollection("PANEL", "BLOCK", raw, signals, nativeComposition, actionSurface, executionSurface);

        assertThat(result).isEqualTo(afterExecution);
        verify(collectionCompositionService, never()).decorateBlock(any(), any(), any(), any(), any());
    }

    @Test
    void metodosDeConstrucaoDeContextoDelegamParaOsColaboradoresReais() {
        Map<String, Object> sharedExperience = Map.of();
        Map<String, Object> signals = Map.of("sig", true);
        Map<String, Object> nativeComposition = Map.of("nat", true);
        Map<String, Object> collectionComposition = Map.of("col", true);
        Map<String, Object> actionSurface = Map.of("act", true);
        List<String> source = List.of("a", "b");
        List<String> composed = List.of("b", "a");

        when(signalReflectionService.deriveSignals("PANEL", sharedExperience, 3, 1, "CTX")).thenReturn(signals);
        when(signalReflectionService.buildNativeComposition("PANEL", signals)).thenReturn(nativeComposition);
        when(collectionCompositionService.composeList("PANEL", "LISTA", source, signals, nativeComposition)).thenReturn(composed);
        when(collectionCompositionService.buildCollectionComposition("PANEL", signals, nativeComposition, Map.of("lista", composed))).thenReturn(collectionComposition);
        when(actionSurfaceCompositionService.buildActionSurface("PANEL", signals, nativeComposition, collectionComposition)).thenReturn(actionSurface);
        when(executionSurfaceCompositionService.buildExecutionSurface("PANEL", signals, nativeComposition, collectionComposition, actionSurface)).thenReturn(Map.of("exe", true));

        assertThat(pipeline.deriveSignals("PANEL", sharedExperience, 3, 1, "CTX")).isEqualTo(signals);
        assertThat(pipeline.buildNativeComposition("PANEL", signals)).isEqualTo(nativeComposition);
        assertThat(pipeline.composeList("PANEL", "LISTA", source, signals, nativeComposition)).isEqualTo(composed);
        assertThat(pipeline.buildCollectionComposition("PANEL", signals, nativeComposition, Map.of("lista", composed))).isEqualTo(collectionComposition);
        assertThat(pipeline.buildActionSurface("PANEL", signals, nativeComposition, collectionComposition)).isEqualTo(actionSurface);
        assertThat(pipeline.buildExecutionSurface("PANEL", signals, nativeComposition, collectionComposition, actionSurface)).isEqualTo(Map.of("exe", true));
    }
}
