package com.tcc.pjb.backend.service.painel.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PainelSignalReflectionServiceTest {

    private final PainelSharedExperienceService sharedExperienceService = new PainelSharedExperienceService();
    private final PainelSignalReflectionService signalReflectionService = new PainelSignalReflectionService();

    @Test
    void deveDerivarSinaisCriticosQuandoHouverPrazoUrgente() {
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("SECRETARIA");
        Map<String, Object> signals = signalReflectionService.deriveSignals("SECRETARIA", sharedExperience, 18, 2, "COORDENACAO_CARTORARIA");
        assertEquals("CRITICO_24H", signals.get("priorityTag"));
        assertEquals("COORDENACAO_CARTORARIA", signals.get("dominantContext"));
        assertTrue(((List<?>) signals.get("suggestedNativeSections")).contains("FILA"));
        assertEquals("FOCUS_FLOW", signals.get("compositionMode"));
        assertEquals("FILA", signals.get("highlightedNativeSection"));
    }

    @Test
    void deveRefletirSinaisNoBlocoNativo() {
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("OFICIAL_JUSTICA");
        Map<String, Object> signals = signalReflectionService.deriveSignals("OFICIAL_JUSTICA", sharedExperience, 8, 0, "CUMPRIMENTO_EXTERNO");
        Map<String, Object> reflected = signalReflectionService.reflectInBlock("OFICIAL_JUSTICA", "AGENDA", Map.of("enabled", true), signals);
        assertTrue(reflected.containsKey("signalReflection"));
        Map<?, ?> reflection = (Map<?, ?>) reflected.get("signalReflection");
        assertEquals("AGENDA", reflection.get("blockName"));
        assertEquals("CUMPRIMENTO_EXTERNO", reflection.get("dominantContext"));
        assertEquals("SECONDARY", reflection.get("displayPriority"));
    }

    @Test
    void deveMontarComposicaoNativaDoPainel() {
        Map<String, Object> sharedExperience = sharedExperienceService.snapshot("MINISTERIO_PUBLICO");
        Map<String, Object> signals = signalReflectionService.deriveSignals("MINISTERIO_PUBLICO", sharedExperience, 20, 0, "ATUACAO_FINALISTICA");
        Map<String, Object> composition = signalReflectionService.buildNativeComposition("MINISTERIO_PUBLICO", signals);
        assertEquals("PRIORITY_FLOW", composition.get("compositionMode"));
        assertTrue(composition.containsKey("sectionOrder"));
        assertEquals(signals.get("priorityTag"), composition.get("priorityTag"));
    }
}
