package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfileBusinessResolverTest {

    @Test
    void mustResolveFalenciaAsBusinessInsolvencyProfile() {
        NationalProceduralActionProfileBusinessResolver resolver = new NationalProceduralActionProfileBusinessResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "pedido de falencia com plano de recuperacao frustrado",
                null
        )).orElseThrow();

        assertEquals("INSOLVENCIA_EMPRESARIAL", profile.actionNature());
        assertEquals("EMPRESARIAL", profile.actionFamily());
        assertEquals("FALENCIA", profile.defaultRito());
    }
}
