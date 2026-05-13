package com.tcc.pjb.backend.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class JurisdictionEngineTest {

    @Test
    void deveMaterializarBuilderExplicitoDaSpecSemLombok() {
        JurisdictionEngine.JurisdictionSpec spec = JurisdictionEngine.JurisdictionSpec.builder()
                .label("Justiça Comum")
                .category("COMUM")
                .rite(JurisdictionEngine.Rite.COMUM)
                .authorities(List.of(
                        JurisdictionEngine.JurisdictionSpec.Authority.builder().name("Vara Cível").level("1o grau").build()
                ))
                .legalBases(List.of(
                        JurisdictionEngine.JurisdictionSpec.LegalBase.builder().citation("CPC").description("processo civil").build()
                ))
                .countries(List.of(
                        JurisdictionEngine.JurisdictionSpec.Country.builder().name("Brasil").iso("BR").build()
                ))
                .treaties(List.of("CADH"))
                .build();

        assertEquals("Justiça Comum", spec.getLabel());
        assertEquals("COMUM", spec.getCategory());
        assertEquals(JurisdictionEngine.Rite.COMUM, spec.getRite());
        assertEquals("Vara Cível", spec.getAuthorities().get(0).getName());
        assertEquals("CPC", spec.getLegalBases().get(0).getCitation());
        assertEquals("BR", spec.getCountries().get(0).getIso());
        assertEquals(List.of("CADH"), spec.getTreaties());
    }

    @Test
    void deveExporGettersExplicitosDoResultado() {
        JurisdictionEngine.Result result = JurisdictionEngine.Result.notFound("sem correspondência", java.util.Map.of("rule", "none"));
        assertFalse(result.isFound());
        assertEquals(0.0, result.getConfidence());
        assertEquals("sem correspondência", result.getReason());
        assertTrue(result.getDebug().containsKey("rule"));
    }
}
