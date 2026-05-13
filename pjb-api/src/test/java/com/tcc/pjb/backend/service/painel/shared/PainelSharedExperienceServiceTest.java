package com.tcc.pjb.backend.service.painel.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PainelSharedExperienceServiceTest {

    private final PainelSharedExperienceService service = new PainelSharedExperienceService();

    @Test
    void deveExporBlocosCompartilhadosParaPainelInstitucional() {
        Map<String, Object> snapshot = service.snapshot("OFICIAL_JUSTICA");
        assertEquals("OFICIAL_JUSTICA", snapshot.get("panelCode"));
        assertTrue(snapshot.containsKey("calendar"));
        assertTrue(snapshot.containsKey("deadlines"));
        assertTrue(snapshot.containsKey("colors"));
        assertTrue(snapshot.containsKey("calculator"));
        assertTrue(snapshot.containsKey("reading"));
        Map<?, ?> calculator = (Map<?, ?>) snapshot.get("calculator");
        assertEquals(Boolean.TRUE, calculator.get("enabled"));
        assertTrue(((List<?>) calculator.get("preferredDomains")).contains("CUSTAS_PROCESSUAIS"));
    }

    @Test
    void deveReduzirDestaqueCalculadoraQuandoPainelNaoForPrimariamenteCalculatorio() {
        Map<String, Object> snapshot = service.snapshot("PSICOSSOCIAL");
        Map<?, ?> calculator = (Map<?, ?>) snapshot.get("calculator");
        assertEquals(Boolean.FALSE, calculator.get("enabled"));
        Map<?, ?> colors = (Map<?, ?>) snapshot.get("colors");
        assertEquals("ACESSIBILIDADE_ESTENDIDA", colors.get("recommendedPersona"));
    }
}
