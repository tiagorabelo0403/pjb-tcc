package com.tcc.pjb.backend.service.painel.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PainelActionSurfaceCompositionServiceTest {

    private final PainelActionSurfaceCompositionService service = new PainelActionSurfaceCompositionService();

    @Test
    void deveConstruirActionSurfaceComAcoesAtalhosECards() {
        Map<String, Object> operationalSignals = new LinkedHashMap<>();
        operationalSignals.put("priorityTag", "CRITICO_24H");
        operationalSignals.put("attentionScore", 93);
        operationalSignals.put("dominantContext", "COORDENACAO_CARTORARIA");

        Map<String, Object> nativeComposition = new LinkedHashMap<>();
        nativeComposition.put("highlightedSection", "INTIMACOES");

        Map<String, Object> collectionComposition = Map.of(
                "collections", Map.of(
                        "intimacoesExpedir", Map.of("size", 12, "emptyState", "READY", "highlighted", true)
                )
        );

        Map<String, Object> actionSurface = service.buildActionSurface(
                "SECRETARIA",
                operationalSignals,
                nativeComposition,
                collectionComposition
        );

        assertEquals("EMERGENCY_MENU", actionSurface.get("menuMode"));
        assertEquals("ATOS_CARTORARIOS", actionSurface.get("dominantActionLane"));
        assertFalse(((List<?>) actionSurface.get("suggestedActions")).isEmpty());
        assertFalse(((List<?>) actionSurface.get("primaryShortcuts")).isEmpty());
        assertFalse(((List<?>) actionSurface.get("priorityCards")).isEmpty());
    }

    @Test
    void deveDecorarBlocoComHintsDeAcoes() {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("items", List.of("Mandado urgente", "Mandado ordinário"));

        Map<String, Object> actionSurface = new LinkedHashMap<>();
        actionSurface.put("menuMode", "ATTENTION_MENU");
        actionSurface.put("dominantActionLane", "CUMPRIMENTO_EXTERNO");
        actionSurface.put("suggestedActions", List.of(
                Map.of("label", "Abrir mandado urgente", "targetHint", "MANDADO", "priorityBoost", true)
        ));
        actionSurface.put("primaryShortcuts", List.of(
                Map.of("label", "Mandados", "path", "/api/v1/oficial-justica/mandados", "targetHint", "MANDADO")
        ));
        actionSurface.put("priorityCards", List.of(
                Map.of("label", "mandados", "targetHint", "MANDADO")
        ));

        Map<String, Object> nativeComposition = Map.of("highlightedSection", "MANDADO");

        Map<String, Object> decorated = service.decorateBlock(
                "OFICIAL_JUSTICA",
                "MANDADOS",
                block,
                actionSurface,
                nativeComposition
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> hints = (Map<String, Object>) decorated.get("actionSurfaceHints");
        assertTrue((Boolean) hints.get("highlighted"));
        assertEquals("ATTENTION_MENU", hints.get("menuMode"));
        assertEquals("CUMPRIMENTO_EXTERNO", hints.get("dominantActionLane"));
    }
}
