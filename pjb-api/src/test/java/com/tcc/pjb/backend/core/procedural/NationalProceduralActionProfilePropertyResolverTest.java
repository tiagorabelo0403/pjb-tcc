package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfilePropertyResolverTest {

    @Test
    void mustResolveUsucapiaoAsRealEstateProfile() {
        NationalProceduralActionProfilePropertyResolver resolver = new NationalProceduralActionProfilePropertyResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "acao de usucapiao extraordinaria de imovel urbano",
                null
        )).orElseThrow();

        assertEquals("USUCAPIAO", profile.actionNature());
        assertEquals("CIVIL_IMOBILIARIO", profile.actionFamily());
        assertEquals("CIVIL_USUCAPIAO", profile.defaultRito());
    }

    @Test
    void mustResolveDivisaoDemarcacaoAsSpecialProcedureOutsideTheJuizados() {
        NationalProceduralActionProfilePropertyResolver resolver = new NationalProceduralActionProfilePropertyResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                NationalProceduralRoutingSupport.normalize("Ação de demarcação de divisa entre imóveis rurais confrontantes"),
                null
        )).orElseThrow();

        assertEquals("DIVISAO_DEMARCACAO", profile.actionNature());
        assertEquals("CIVIL_DIVISAO_DEMARCACAO", profile.defaultRito());
        assertEquals(true, profile.specialProcedure());
        assertEquals(true, profile.alerts().stream().anyMatch(alerta -> alerta.contains("Lei 10.259")));
    }
}
