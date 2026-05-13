package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NationalProceduralDistributionResolverTest {

    @Test
    void mustReturnEmptyWhenTerritorialBaseIsMissing() {
        NationalProceduralDistributionResolver resolver = new NationalProceduralDistributionResolver(mock(MapaCompetenciaDinamicoEngine.class));

        Optional<NationalProceduralDistributionSuggestion> suggestion = resolver.resolve(context(Map.of(), null, "CE"));

        assertTrue(suggestion.isEmpty());
    }

    @Test
    void mustTranslateDynamicDistributionResponse() {
        MapaCompetenciaDinamicoEngine engine = mock(MapaCompetenciaDinamicoEngine.class);
        when(engine.distribuir(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(
                new DynamicCompetenceDistributionResponse(
                        "req",
                        Instant.now(),
                        "0001",
                        "VARA-01",
                        "TJCE",
                        "Fortaleza",
                        "CE",
                        "CIVEL",
                        91.2d,
                        true,
                        "melhor aderencia",
                        List.of("alerta operacional"),
                        List.of("validar"),
                        List.of(),
                        null
                )
        ));
        NationalProceduralDistributionResolver resolver = new NationalProceduralDistributionResolver(engine);

        NationalProceduralDistributionSuggestion suggestion = resolver.resolve(context(Map.of("assunto", "Obrigacao de fazer"), "Fortaleza", "CE")).orElseThrow();

        assertEquals("VARA-01", suggestion.unidadeCodigo());
        assertEquals("TJCE", suggestion.tribunalCodigo());
        assertEquals("Fortaleza", suggestion.comarca());
        assertEquals("CE", suggestion.uf());
    }

    private static NationalProceduralDistributionContext context(Map<String, Object> payload, String cidade, String uf) {
        return new NationalProceduralDistributionContext(
                payload,
                new ProceduralCanonicalResolver.CanonicalContext(
                        Instant.now(),
                        RitoProcessual.COMUM_ORDINARIO,
                        "CIVIL",
                        "7",
                        "Procedimento Comum",
                        "ESTADUAL",
                        "TJCE",
                        "TJCE",
                        "PJE",
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of()
                ),
                new CompetenceResolveResponse("req", Instant.now(), "ESTADUAL", "COMUM_ORDINARIO", 0.85d, List.of(), List.of(), Map.of()),
                "COMUM_ORDINARIO",
                TipoJustica.ESTADUAL,
                new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of(), List.of(), 0.82d, false),
                cidade,
                uf,
                new NationalProceduralActionProfile("INDENIZATORIA", "CIVEL", false, "COMUM_ORDINARIO", "CIVEL", List.of(), List.of(), List.of(), List.of(), List.of())
        );
    }
}
