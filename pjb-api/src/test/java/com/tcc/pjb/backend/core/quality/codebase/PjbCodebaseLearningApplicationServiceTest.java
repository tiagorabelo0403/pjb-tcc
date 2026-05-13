package com.tcc.pjb.backend.core.quality.codebase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseLearningApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseLearningAggregate;
import org.junit.jupiter.api.Test;

class PjbCodebaseLearningApplicationServiceTest {

    @Test
    void deveMapearHotspotsDoCoreComOndasEPistasDeExtracao() {
        PjbCodebaseLearningApplicationService service = new PjbCodebaseLearningApplicationService();
        PjbCodebaseLearningAggregate aggregate = service.aprender();

        assertTrue(aggregate.disponivel());
        assertTrue(aggregate.arquivosMain() > 0);
        assertTrue(aggregate.fatiasMapeadas() > 0);
        assertFalse(aggregate.hotspots().isEmpty());
        assertFalse(aggregate.blueprintsExtracao().isEmpty());
        assertFalse(aggregate.fluxosCriticos().isEmpty());
        assertFalse(aggregate.ondasPrioritarias().isEmpty());
        assertFalse(aggregate.aprendizados().isEmpty());
        assertTrue(aggregate.testesIntegracao() > 0);
        assertTrue(aggregate.hotspots().stream().allMatch(item -> item.fatia().startsWith("core/")));
        assertTrue(aggregate.hotspots().stream().allMatch(item -> item.pressaoExtracao() >= 0));
        assertTrue(aggregate.hotspots().stream().anyMatch(item -> item.razaoTeste() < 0.20d));
        assertTrue(aggregate.hotspots().stream().anyMatch(item -> !item.trilhasExtracao().isEmpty()));
    }
}
