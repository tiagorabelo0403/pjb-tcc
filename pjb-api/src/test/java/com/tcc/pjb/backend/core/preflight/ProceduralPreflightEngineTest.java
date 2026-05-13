package com.tcc.pjb.backend.core.preflight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProceduralPreflightEngineTest {

    @Test
    void blocksSubmissionWhenCanonicalContextIsIncomplete() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);
        ProceduralPreflightEngine engine = new ProceduralPreflightEngine(resolver, gate);

        var result = engine.evaluate(new ProceduralPreflightEngine.PreflightContext(
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
                false,
                false,
                false,
                false,
                false,
                Map.of()
        ));

        assertTrue(result.hasBlockers());
        assertEquals("CANONICAL_CONTEXT_INCOMPLETE", result.metadata().get("sanityStatus"));
        assertTrue(result.issues().stream().map(ProceduralPreflightEngine.PreflightIssue::code).anyMatch("GATE_CANONICAL_CONTEXT_INCOMPLETE"::equals));
    }

    @Test
    void resolvesLegacyAliasesIntoCanonicalElectoralPreflight() {
        ProceduralCatalogService catalogService = new ProceduralCatalogService();
        ProceduralCanonicalResolver resolver = new ProceduralCanonicalResolver(catalogService);
        CanonicalSanityGate gate = new CanonicalSanityGate(catalogService);
        ProceduralPreflightEngine engine = new ProceduralPreflightEngine(resolver, gate);

        var ctx = ProceduralPreflightEngine.PreflightContext.fromMap(Map.of(
                "procedimento", "ELEITORAL",
                "estado", "CE",
                "municipio", "Fortaleza",
                "documentos", java.util.List.of("PROCURACAO")
        ));
        var result = engine.evaluate(ctx);

        assertEquals("ELEITORAL", result.resolvedRito());
        assertEquals("TRE-CE", result.resolvedTribunalCodigo());
        assertTrue(((Map<?, ?>) result.metadata().get("canonicalContext")).containsKey("rito"));
    }
}
