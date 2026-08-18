package com.tcc.pjb.backend.service.jurisprudencia.graph;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.model.entity.jurisprudencia.PrecedenteEdge;
import com.tcc.pjb.backend.model.repository.PrecedenteEdgeRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class JurisprudenceGraphService {

    private final CitationExtractor extractor;
    private final PrecedenteEdgeRepository edgeRepository;

    public JurisprudenceGraphService(CitationExtractor extractor,
                                    PrecedenteEdgeRepository edgeRepository) {
        this.extractor = extractor;
        this.edgeRepository = edgeRepository;
    }

    @PjbTransactionalBudget(operation = "jurisprudencia.graph.upsert-edges-for", maxMillis = 5000)
    @Transactional
    public void upsertEdgesFor(Precedente precedente) {
        if (precedente == null || precedente.getId() == null) return;

        
        edgeRepository.deleteByFromPrecedenteId(precedente.getId());

        List<CitationRef> refs = extractor.extract(precedente.getTitulo(), precedente.getTese(), precedente.getEmentaResumo());
        if (refs.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        List<PrecedenteEdge> edges = refs.stream()
                .filter(Objects::nonNull)
                .map(r -> PrecedenteEdge.builder()
                        .fromPrecedente(precedente)
                        .relation(r.relation() == null ? CitationRelationType.CITES.name() : r.relation().name())
                        .targetType(r.targetType() == null ? CitationTargetType.OUTRO.name() : r.targetType().name())
                        .targetRef(limit(r.targetRef(), 220))
                        .raw(limit(r.raw(), 260))
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();

        edgeRepository.saveAll(edges);
    }

    private static String limit(String v, int max) {
        if (v == null) return null;
        String s = v.trim();
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}
