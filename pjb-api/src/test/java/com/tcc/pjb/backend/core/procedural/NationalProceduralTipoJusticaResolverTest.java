package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralTipoJusticaResolverTest {

    @Test
    void mustPreferExplicitTipoJusticaWhenPresent() {
        NationalProceduralTipoJusticaResolver resolver = new NationalProceduralTipoJusticaResolver();

        TipoJustica tipoJustica = resolver.resolve(
                "TRABALHO",
                competence("ESTADUAL"),
                canonical("ESTADUAL"),
                "COMUM_ORDINARIO",
                new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, false, List.of(), "AUTOR", "REU")
        );

        assertEquals(TipoJustica.TRABALHO, tipoJustica);
    }

    @Test
    void mustInferFederalFromRitoWhenInputIsMissing() {
        NationalProceduralTipoJusticaResolver resolver = new NationalProceduralTipoJusticaResolver();

        TipoJustica tipoJustica = resolver.resolve(
                null,
                competence(null),
                canonical(null),
                "JEF_PREVIDENCIARIO",
                new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, false, List.of(), "AUTOR", "REU")
        );

        assertEquals(TipoJustica.FEDERAL, tipoJustica);
    }

    @Test
    void mustFallbackToPartyProfileWhenNoDirectSignalExists() {
        NationalProceduralTipoJusticaResolver resolver = new NationalProceduralTipoJusticaResolver();

        TipoJustica tipoJustica = resolver.resolve(
                null,
                competence(null),
                canonical(null),
                null,
                new NationalProceduralPartyProfile(false, false, false, false, false, true, false, false, false, List.of("RELACAO_TRABALHO"), "AUTOR", "REU")
        );

        assertEquals(TipoJustica.TRABALHO, tipoJustica);
    }

    private static CompetenceResolveResponse competence(String tipoJustica) {
        return new CompetenceResolveResponse("req", Instant.now(), tipoJustica, "COMUM_ORDINARIO", 0.8d, List.of(), List.of(), Map.of());
    }

    private static ProceduralCanonicalResolver.CanonicalContext canonical(String ramoJustica) {
        return new ProceduralCanonicalResolver.CanonicalContext(
                Instant.now(),
                RitoProcessual.COMUM_ORDINARIO,
                "CIVIL",
                "7",
                "Procedimento Comum",
                ramoJustica,
                "TJCE",
                "TJCE",
                "PJE",
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
    }
}
