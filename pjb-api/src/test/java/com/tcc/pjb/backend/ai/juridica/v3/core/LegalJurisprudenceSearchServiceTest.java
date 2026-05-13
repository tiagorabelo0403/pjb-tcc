package com.tcc.pjb.backend.ai.juridica.v3.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationExtractor;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationRef;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationRelationType;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationTargetType;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchEngine;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchHit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalJurisprudenceSearchServiceTest {

    @Test
    void searchBuildsNullSafePayloadAndPassesFilters() {
        JurisprudenceSearchEngine engine = mock(JurisprudenceSearchEngine.class);
        CitationExtractor extractor = mock(CitationExtractor.class);
        LegalJurisprudenceSearchService service = new LegalJurisprudenceSearchService(engine, extractor);

        JurisprudenceSearchHit hit = new JurisprudenceSearchHit(
                10L,
                null,
                null,
                null,
                "Tema repetitivo",
                null,
                "Resumo",
                null,
                null,
                null,
                null,
                1.25
        );

        when(engine.search(eq("prescricao intercorrente"), eq(RamoDireito.CIVIL), eq("JUIZADO_ESPECIAL_CIVEL"), eq(10)))
                .thenReturn(List.of(hit));
        when(extractor.extract(eq("Tema repetitivo"), eq(null), eq("Resumo")))
                .thenReturn(List.of(new CitationRef(CitationRelationType.CITES, CitationTargetType.PRECEDENTE, "ref-1", "REsp 1")));

        List<Map<String, Object>> out = service.search("prescricao intercorrente", "civil", "JUIZADO_ESPECIAL_CIVEL");

        assertEquals(1, out.size());
        Map<String, Object> payload = out.get(0);
        assertEquals(10L, payload.get("id"));
        assertEquals("Tema repetitivo", payload.get("titulo"));
        assertEquals("Resumo", payload.get("ementaResumo"));
        assertFalse(payload.containsKey("fonte"));
        assertFalse(payload.containsKey("tipo"));
        verify(engine).search(eq("prescricao intercorrente"), eq(RamoDireito.CIVIL), eq("JUIZADO_ESPECIAL_CIVEL"), eq(10));
    }
}
