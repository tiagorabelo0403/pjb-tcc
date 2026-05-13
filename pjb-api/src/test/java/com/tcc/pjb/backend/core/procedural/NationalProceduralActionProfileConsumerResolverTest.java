package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfileConsumerResolverTest {

    @Test
    void mustCloseConsumoAsCivilConsumptionProfile() {
        NationalProceduralActionProfileConsumerResolver resolver = new NationalProceduralActionProfileConsumerResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "acao do consumidor com negativacao indevida e dano moral",
                null
        ));

        assertEquals("INDENIZATORIA", profile.actionNature());
        assertEquals("CIVIL_CONSUMO", profile.actionFamily());
        assertEquals("CIVEL", profile.varaFamily());
    }

    @Test
    void mustPreservePublicEntityMarkerOnGeneralCivilFallback() {
        NationalProceduralActionProfileConsumerResolver resolver = new NationalProceduralActionProfileConsumerResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "acao de responsabilidade contratual sem marcador especial",
                new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, true, java.util.List.of("ESTADO"), "AUTOR", "REU")
        ));

        assertEquals("ACAO_CIVEL_GERAL", profile.actionNature());
        assertTrue(profile.markers().contains("LITIGIO_COM_PODER_PUBLICO"));
    }
}
