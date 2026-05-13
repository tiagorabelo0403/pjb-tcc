package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalSanityGateTest {

    @Test
    void blocksWhenCompetenceDoesNotReachTribunal() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);

        var canonical = resolver.resolve(Map.of("rito", "COMUM_ORDINARIO"));
        var result = gate.evaluate(canonical);

        assertFalse(result.passed());
        assertTrue(result.statusCodes().contains(CanonicalSanityGate.Status.COMPETENCE_UNRESOLVED.name()));
    }
}
