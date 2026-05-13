package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class CanonicalRitoSelectorTest {

    @Test
    void prefersCanonicalRitoWhenCatalogSignalIsComplete() {
        CanonicalRitoSelector selector = new CanonicalRitoSelector(
                new ProceduralCanonicalResolver(new ProceduralCatalogService()),
                new CanonicalSanityGate(new ProceduralCatalogService())
        );
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("rito", "COMUM_ORDINARIO");
        payload.put("classe", "Procedimento Comum");
        payload.put("tribunalCodigo", "TJCE");
        payload.put("ramoDireito", "CIVIL");

        var selected = selector.select(payload, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, "test");

        assertEquals(RitoProcessual.COMUM_ORDINARIO, selected.rito());
        assertFalse(selected.heuristicUsed());
        assertFalse(selected.fallbackApplied());
    }

    @Test
    void usesHeuristicCompatibilityWhenCanonicalRitoIsUnavailable() {
        CanonicalRitoSelector selector = new CanonicalRitoSelector(
                new ProceduralCanonicalResolver(new ProceduralCatalogService()),
                new CanonicalSanityGate(new ProceduralCatalogService())
        );

        var selected = selector.select(new LinkedHashMap<>(), RitoProcessual.ELEITORAL_AIJE, "test");

        assertEquals(RitoProcessual.ELEITORAL_AIJE, selected.rito());
        assertTrue(selected.heuristicUsed());
        assertFalse(selected.fallbackApplied());
    }

    @Test
    void parsesStringHeuristicWithoutDirectEnumCallSite() {
        CanonicalRitoSelector selector = new CanonicalRitoSelector(
                new ProceduralCanonicalResolver(new ProceduralCatalogService()),
                new CanonicalSanityGate(new ProceduralCatalogService())
        );

        var selected = selector.select(new LinkedHashMap<>(), "ELEITORAL_AIJE", "test");

        assertEquals(RitoProcessual.ELEITORAL_AIJE, selected.rito());
        assertTrue(selected.heuristicUsed());
    }

}
