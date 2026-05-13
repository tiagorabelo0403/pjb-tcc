package com.tcc.pjb.backend.service.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProceduralLegacyCompatibilityContractTest {

    private final ProceduralCatalogService catalogService = new ProceduralCatalogService();

    @Test
    void legacyAliasesMustResolveToSameCanonicalRito() {
        RitoProcessual byLegacyPayload = catalogService.resolveRito(Map.of(
                "procedimento", "ELEITORAL",
                "materia", "eleitoral"
        ));
        RitoProcessual byCanonicalPayload = catalogService.resolveRito(Map.of(
                "rito", "ELEITORAL",
                "ramoDireito", "ELEITORAL"
        ));

        assertEquals(RitoProcessual.ELEITORAL, byLegacyPayload);
        assertEquals(byCanonicalPayload, byLegacyPayload);
    }

    @Test
    void legacyClasseAliasMustStillResolveClasseTpuAgainstCanonicalRito() {
        var resolved = catalogService.resolveClasseTpu(Map.of(
                "classeProcessual", "ELEITORAL_REGISTRO_CANDIDATURA",
                "materia", "eleitoral"
        ));

        assertTrue(resolved.isPresent());
    }
}
