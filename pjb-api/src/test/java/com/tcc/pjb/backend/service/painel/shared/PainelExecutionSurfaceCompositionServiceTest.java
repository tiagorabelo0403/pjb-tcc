package com.tcc.pjb.backend.service.painel.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PainelExecutionSurfaceCompositionServiceTest {

    private final PainelExecutionSurfaceCompositionService service = new PainelExecutionSurfaceCompositionService();

    @Test
    void deveConstruirExecutionSurfaceComCtaWorkflowTransacoesEComandos() {
        Map<String, Object> operationalSignals = new LinkedHashMap<>();
        operationalSignals.put("priorityTag", "CRITICO_24H");
        operationalSignals.put("attentionScore", 91);
        operationalSignals.put("dominantContext", "CUMPRIMENTO_EXTERNO");

        Map<String, Object> nativeComposition = Map.of("highlightedSection", "MANDADOS");
        Map<String, Object> collectionComposition = Map.of(
                "collections", Map.of(
                        "proximosMandados", Map.of("size", 7, "emptyState", "READY")
                )
        );
        Map<String, Object> actionSurface = Map.of("dominantActionLane", "CUMPRIMENTO_EXTERNO");

        Map<String, Object> executionSurface = service.buildExecutionSurface(
                "OFICIAL_JUSTICA",
                operationalSignals,
                nativeComposition,
                collectionComposition,
                actionSurface
        );

        assertEquals("GUIDED_EXECUTION", executionSurface.get("executionMode"));
        assertEquals("CUMPRIMENTO_EXTERNO", executionSurface.get("dominantTransactionLane"));
        assertFalse(((List<?>) executionSurface.get("standardTransactions")).isEmpty());
        assertFalse(((List<?>) executionSurface.get("quickCommands")).isEmpty());
    }

    @Test
    void deveDecorarBlocoComHintsDeExecucao() {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("items", List.of("Mandado urgente"));
        Map<String, Object> executionSurface = new LinkedHashMap<>();
        executionSurface.put("executionMode", "ACCELERATED_EXECUTION");
        executionSurface.put("dominantTransactionLane", "CUMPRIMENTO_EXTERNO");
        executionSurface.put("primaryCta", Map.of("label", "Abrir mandado urgente"));
        executionSurface.put("workflowStarter", Map.of("code", "CUMPRIMENTO_EXTERNO_FLOW"));
        executionSurface.put("commandGroupingMode", "TACTICAL_CLUSTER");
        executionSurface.put("standardTransactions", List.of(Map.of("label", "Registrar diligência", "targetHint", "MANDADO")));
        executionSurface.put("quickCommands", List.of(Map.of("label", "Abrir comando rápido", "targetHint", "MANDADO")));
        Map<String, Object> nativeComposition = Map.of("highlightedSection", "MANDADOS");

        Map<String, Object> decorated = service.decorateBlock(
                "OFICIAL_JUSTICA",
                "MANDADOS",
                block,
                executionSurface,
                nativeComposition
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> hints = (Map<String, Object>) decorated.get("executionSurfaceHints");
        assertTrue((Boolean) hints.get("highlighted"));
        assertEquals("TACTICAL_CLUSTER", hints.get("quickCommandMode"));
        assertEquals("Abrir mandado urgente", hints.get("primaryCtaLabel"));
    }
}
