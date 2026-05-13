package com.tcc.pjb.backend.service.semantic;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.repository.PrecedenteRepository;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;

@Service
public class SemanticPrecedentSearchService {

    private static final int MAX_BOOTSTRAPPED_KEYS = 256;

    private final PrecedenteRepository precedenteRepository;
    private final EmbeddingService embeddings;
    private final VectorIndex vectorIndex;
    private final ProceduralCatalogService proceduralCatalogService;
    private final HybridPrecedentRanker hybridPrecedentRanker;
    private final ConcurrentHashMap<String, Long> bootstrapped = new ConcurrentHashMap<>();

    public SemanticPrecedentSearchService(PrecedenteRepository precedenteRepository,
                                         EmbeddingService embeddings,
                                         VectorIndex vectorIndex,
                                         ProceduralCatalogService proceduralCatalogService,
                                         HybridPrecedentRanker hybridPrecedentRanker) {
        this.precedenteRepository = precedenteRepository;
        this.embeddings = embeddings;
        this.vectorIndex = vectorIndex;
        this.proceduralCatalogService = proceduralCatalogService;
        this.hybridPrecedentRanker = hybridPrecedentRanker;
    }

    public List<Precedente> semanticSearch(RamoDireito ramo, String ritoName, String query, int topK) {
        RitoProcessual rito = ritoName == null || ritoName.isBlank() ? null : proceduralCatalogService.resolveRito(ritoName, ramo != null ? ramo.name() : null, null);
        return semanticSearch(ramo, rito, query, topK);
    }

    public List<Precedente> semanticSearch(RamoDireito ramo, RitoProcessual rito, String query, int topK) {
        if (query == null || query.isBlank()) return List.of();
        String key = (ramo != null ? ramo.name() : "ANY") + "|" + (rito != null ? rito.name() : "ANY");
        bootstrapIfNeeded(key, ramo, rito);

        var qv = embeddings.embed(query);
        List<VectorSearchHit> hits = vectorIndex.search(qv, Math.max(1, topK), Map.of(
                "ramo", ramo != null ? ramo.name() : "ANY",
                "rito", rito != null ? rito.name() : "ANY"
        ));

        if (hits.isEmpty()) return List.of();
        List<Long> ids = new ArrayList<>(hits.size());
        for (VectorSearchHit h : hits) {
            try {
                ids.add(Long.parseLong(h.id()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (ids.isEmpty()) return List.of();
        List<Precedente> found = precedenteRepository.findAllById(ids);
        List<SemanticRankedHit> ranked = hybridPrecedentRanker.rank(query, hits, found, ramo, rito, Math.max(1, topK));
        return ranked.stream().map(SemanticRankedHit::precedente).toList();
    }

    private void bootstrapIfNeeded(String key, RamoDireito ramo, RitoProcessual rito) {
        long now = System.nanoTime();
        Long known = bootstrapped.get(key);
        if (known != null) {
            bootstrapped.put(key, now);
            return;
        }

        var page = precedenteRepository.search(null, null, ramo, rito, null, PageRequest.of(0, 250));
        for (Precedente p : page.getContent()) {
            String text = (p.getTitulo() != null ? p.getTitulo() : "") + "\n" + (p.getTese() != null ? p.getTese() : "");
            var vec = embeddings.embed(text);
            vectorIndex.upsert(String.valueOf(p.getId()), vec, Map.of(
                    "ramo", ramo != null ? ramo.name() : "ANY",
                    "rito", rito != null ? rito.name() : "ANY"
            ));
        }

        bootstrapped.put(key, now);
        trimBootstrapped();
    }

    private void trimBootstrapped() {
        int overflow = bootstrapped.size() - MAX_BOOTSTRAPPED_KEYS;
        if (overflow <= 0) {
            return;
        }
        List<Map.Entry<String, Long>> entries = new ArrayList<>(bootstrapped.entrySet());
        entries.sort(Map.Entry.comparingByValue());
        for (Map.Entry<String, Long> entry : entries) {
            if (overflow <= 0) {
                break;
            }
            if (bootstrapped.remove(entry.getKey(), entry.getValue())) {
                overflow--;
            }
        }
    }
}
