package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfileFamilyResolverTest {

    @Test
    void mustResolveInventarioAsSuccessionProfile() {
        NationalProceduralActionProfileFamilyResolver resolver = new NationalProceduralActionProfileFamilyResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "pedido de inventario e partilha de heranca",
                null
        )).orElseThrow();

        assertEquals("INVENTARIO_ARROLAMENTO", profile.actionNature());
        assertEquals("CIVIL_SUCESSOES", profile.actionFamily());
        assertTrue(profile.specialProcedure());
    }
}
