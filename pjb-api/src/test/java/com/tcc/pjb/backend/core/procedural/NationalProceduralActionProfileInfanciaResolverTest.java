package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfileInfanciaResolverTest {

    @Test
    void mustClassifyAdocaoWithDedicatedTrack() {
        NationalProceduralActionProfileInfanciaResolver resolver = new NationalProceduralActionProfileInfanciaResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "pedido de adocao com habilitacao a adocao e estagio de convivencia perante a vara da infancia",
                null
        )).orElseThrow();

        assertEquals("INFANCIA_JUVENTUDE_ADOCAO", profile.defaultRito());
        assertTrue(profile.reviewChecklist().contains("Validar habilitação, estudo psicossocial, estágio de convivência e documentação civil mínima da criança e dos pretendentes."));
    }

    @Test
    void mustClassifyAtoInfracionalBeforeGenericEcaProtection() {
        NationalProceduralActionProfileInfanciaResolver resolver = new NationalProceduralActionProfileInfanciaResolver(
                new NationalProceduralActionProfileMessages()
        );

        NationalProceduralActionProfile profile = resolver.resolve(new NationalProceduralActionProfileContext(
                Map.of(),
                NationalProceduralRoutingTestFixtures.sampleResolution().canonical(),
                "apuracao de ato infracional com medida socioeducativa e internacao provisoria de adolescente",
                null
        )).orElseThrow();

        assertEquals("INFANCIA_JUVENTUDE_INFRACIONAL", profile.defaultRito());
        assertTrue(profile.alerts().contains("Ato infracional exige sigilo reforçado, prioridade absoluta e observância da escuta protegida e das garantias socioeducativas."));
    }


    @Test
    void catalogMustExposeInfanciaSpecializedDocuments() {
        var adocao = ProceduralCatalogSupport.snapshot(com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.INFANCIA_JUVENTUDE_ADOCAO);
        var infracional = ProceduralCatalogSupport.snapshot(com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL);

        assertTrue(adocao.documents().stream().anyMatch(doc -> doc.code().name().equals("HABILITACAO_ADOTANTE") && doc.required()));
        assertTrue(infracional.documents().stream().anyMatch(doc -> doc.code().name().equals("BOLETIM_OU_REPRESENTACAO") && doc.required()));
    }

}
