package com.tcc.pjb.backend.service.painel.shared;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PainelCompositionNullSafetyTest {

    private final PainelSignalReflectionService signalReflectionService = new PainelSignalReflectionService();
    private final PainelNativeCollectionCompositionService nativeCollectionCompositionService = new PainelNativeCollectionCompositionService();
    private final PainelActionSurfaceCompositionService actionSurfaceCompositionService = new PainelActionSurfaceCompositionService();
    private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService = new PainelExecutionSurfaceCompositionService();

    @Test
    void deveDerivarSinaisComSharedExperienceAusenteSemExplodir() {
        Map<String, Object> signals = assertDoesNotThrow(() ->
                signalReflectionService.deriveSignals("OFICIAL_JUSTICA", null, 0, 0, null)
        );
        assertEquals("NORMAL", signals.get("priorityTag"));
        assertEquals("GERAL", signals.get("calendarLane"));
        assertNotNull(signals.get("statusTags"));
    }

    @Test
    void deveConstruirComposicaoDeColecoesComParametrosParciais() {
        Map<String, Object> composition = assertDoesNotThrow(() ->
                nativeCollectionCompositionService.buildCollectionComposition("SECRETARIA", null, null, Map.of("fila", List.of("item")))
        );
        assertEquals("NORMAL", composition.get("priorityTag"));
        @SuppressWarnings("unchecked")
        Map<String, Object> collections = (Map<String, Object>) composition.get("collections");
        assertTrue(collections.containsKey("fila"));
    }

    @Test
    void deveConstruirActionSurfaceComMapasAusentesSemNuloInterno() {
        Map<String, Object> actionSurface = assertDoesNotThrow(() ->
                actionSurfaceCompositionService.buildActionSurface("DELEGADO", null, null, null)
        );
        assertEquals("STANDARD_MENU", actionSurface.get("menuMode"));
        assertNotNull(actionSurface.get("priorityCards"));
        assertNotNull(actionSurface.get("suggestedActions"));
    }

    @Test
    void deveConstruirExecutionSurfaceComSuperficiesAusentesSemFalhar() {
        Map<String, Object> executionSurface = assertDoesNotThrow(() ->
                executionSurfaceCompositionService.buildExecutionSurface("MINISTERIO_PUBLICO", null, null, null, null)
        );
        assertEquals("STANDARD_EXECUTION", executionSurface.get("executionMode"));
        assertNotNull(executionSurface.get("primaryCta"));
        assertNotNull(executionSurface.get("itemCommandPolicy"));
    }
}
