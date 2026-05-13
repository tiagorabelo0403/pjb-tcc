package com.tcc.pjb.backend.service.competencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CompetenceResolverServiceTest {

    @Test
    void resolvesElectoralCompetenceFromStructuredSignals() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);
        CompetenceResolverService service = new CompetenceResolverService(resolver, gate);

        var response = service.resolve(new CompetenceResolveRequest(
                "registro de candidatura com impugnacao e propaganda irregular",
                "registro de candidatura",
                "ELEITORAL_REGISTRO_CANDIDATURA",
                "eleitoral",
                "CE",
                "Fortaleza",
                new BigDecimal("1000.00"),
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                false
        ));

        assertEquals("ELEITORAL", response.tipoJusticaSugerida());
        assertEquals("ELEITORAL", response.ritoSugerido());
        assertTrue(response.confidence() > 0.70d);
        assertTrue(response.debug().containsKey("canonicalContext"));
        assertEquals("TRE-CE", ((Map<?, ?>) response.debug().get("canonicalContext")).get("tribunalCodigo"));
    }

    @Test
    void exposesCanonicalIncompletenessInDebugWhenSignalsAreInsufficient() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);
        CompetenceResolverService service = new CompetenceResolverService(resolver, gate);

        var response = service.resolve(new CompetenceResolveRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertTrue(response.reasons().stream().anyMatch(reason -> reason.contains("Gate canônico")));
        assertEquals("CANONICAL_CONTEXT_INCOMPLETE", response.debug().get("sanityStatus"));
    }
}
