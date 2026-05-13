package com.tcc.pjb.backend.service.painel.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PainelNativeCollectionCompositionServiceTest {

    private final PainelSharedExperienceService sharedExperienceService = new PainelSharedExperienceService();
    private final PainelSignalReflectionService signalReflectionService = new PainelSignalReflectionService();
    private final PainelNativeCollectionCompositionService collectionCompositionService = new PainelNativeCollectionCompositionService();

    @Test
    void deveReordenarColecaoComUrgenciaNaFrente() {
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("SECRETARIA");
        Map<String, Object> signals = signalReflectionService.deriveSignals("SECRETARIA", sharedExperience, 20, 2, "COORDENACAO_CARTORARIA");
        Map<String, Object> nativeComposition = signalReflectionService.buildNativeComposition("SECRETARIA", signals);
        List<String> composed = collectionCompositionService.composeList(
                "SECRETARIA",
                "INTIMACOES_EXPEDIR",
                List.of("rotina ordinaria", "prazo urgente 24h", "mandado simples"),
                signals,
                nativeComposition
        );
        assertEquals("prazo urgente 24h", composed.get(0));
    }

    @Test
    void deveDecorarBlocoComDiretivasDeColecao() {
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("OFICIAL_JUSTICA");
        Map<String, Object> signals = signalReflectionService.deriveSignals("OFICIAL_JUSTICA", sharedExperience, 9, 1, "CUMPRIMENTO_EXTERNO");
        Map<String, Object> nativeComposition = signalReflectionService.buildNativeComposition("OFICIAL_JUSTICA", signals);
        Map<String, Object> decorated = collectionCompositionService.decorateBlock(
                "OFICIAL_JUSTICA",
                "AGENDA",
                Map.of("items", List.of("cumprimento ordinario", "mandado urgente")),
                signals,
                nativeComposition
        );
        assertTrue(decorated.containsKey("collectionComposition"));
        Map<?, ?> directives = (Map<?, ?>) decorated.get("collectionComposition");
        assertEquals("OFICIAL_JUSTICA", directives.get("panelCode"));
    }
}
