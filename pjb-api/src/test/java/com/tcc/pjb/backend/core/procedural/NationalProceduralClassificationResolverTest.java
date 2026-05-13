package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalProceduralClassificationResolverTest {

    private final NationalProceduralClassificationResolver resolver = new NationalProceduralClassificationResolver();

    @Test
    void mustResolveJuizadoRegimeWhenDecisionOverridesTrack() {
        NationalProceduralActionProfile actionProfile = new NationalProceduralActionProfile(
                "INDENIZATORIA",
                "CIVIL_GERAL",
                false,
                "COMUM_ORDINARIO",
                "Vara Cível",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        NationalProceduralJuizadoDecision juizadoDecision = new NationalProceduralJuizadoDecision(
                true,
                "JUIZADO_ESPECIAL_CIVEL",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0.91d,
                false
        );

        assertEquals("JUIZADO", resolver.resolveProceduralRegime("COMUM_ORDINARIO", actionProfile, juizadoDecision));
        assertEquals("JUIZADO_ESPECIAL_CIVEL", resolver.resolveProceduralTrack(null, actionProfile, juizadoDecision, TipoJustica.ESTADUAL));
    }

    @Test
    void mustResolveLaborAndMilitaryTracksFromJusticeTypeWhenRitoMissing() {
        NationalProceduralActionProfile actionProfile = new NationalProceduralActionProfile(
                "RECLAMATORIA",
                "TRABALHISTA",
                false,
                null,
                "Vara do Trabalho",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        NationalProceduralJuizadoDecision juizadoDecision = new NationalProceduralJuizadoDecision(
                false,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0.80d,
                false
        );

        assertEquals("TRABALHISTA_ORDINARIO", resolver.resolveProceduralTrack(null, actionProfile, juizadoDecision, TipoJustica.TRABALHO));
        assertEquals("MILITAR", resolver.resolveProceduralTrack(null, actionProfile, juizadoDecision, TipoJustica.MILITAR_ESTADUAL));
    }
}
