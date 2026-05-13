package com.tcc.pjb.backend.ai.juridica.v3.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationExtractor;
import com.tcc.pjb.backend.service.jurisprudencia.graph.CitationRef;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchEngine;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceSearchHit;

@Service
public class LegalJurisprudenceSearchService {

    private final JurisprudenceSearchEngine engine;
    private final CitationExtractor citationExtractor;

    public LegalJurisprudenceSearchService(JurisprudenceSearchEngine engine,
                                          CitationExtractor citationExtractor) {
        this.engine = engine;
        this.citationExtractor = citationExtractor;
    }

    public List<Map<String, Object>> search(String query) {
        return search(query, null, null);
    }

    public List<Map<String, Object>> search(String query, String ramoName, String ritoName) {
        RamoDireito ramo = parseRamo(ramoName);
        List<JurisprudenceSearchHit> hits = engine.search(query, ramo, ritoName, 10);
        if (hits == null) return List.of();
        if (hits.isEmpty()) return List.of();

        ArrayList<Map<String, Object>> out = new ArrayList<>(hits.size());
        for (JurisprudenceSearchHit h : hits) {
            List<CitationRef> extracted = citationExtractor.extract(h.titulo(), h.tese(), h.ementaResumo());
            List<String> citations = (extracted == null ? List.<CitationRef>of() : extracted).stream()
                    .map(r -> r.targetRef())
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(12)
                    .toList();

            out.add(PayloadMaps.ofEntries(
                    "id", h.id(),
                    "fonte", h.fonte() != null ? h.fonte().name() : null,
                    "tipo", h.tipo() != null ? h.tipo().name() : null,
                    "identificador", h.identificador(),
                    "titulo", h.titulo(),
                    "tese", h.tese(),
                    "ementaResumo", h.ementaResumo(),
                    "urlReferencia", h.urlReferencia(),
                    "dataPublicacao", h.dataPublicacao() != null ? h.dataPublicacao().toString() : null,
                    "ramoSugerido", h.ramoSugerido() != null ? h.ramoSugerido().name() : null,
                    "ritoSugerido", h.ritoSugerido() != null ? h.ritoSugerido().name() : null,
                    "score", h.score(),
                    "citations", citations
            ));
        }
        return out;
    }

    private static RamoDireito parseRamo(String ramoName) {
        if (ramoName == null || ramoName.isBlank()) {
            return null;
        }
        try {
            return RamoDireito.valueOf(ramoName.trim().toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }
}
