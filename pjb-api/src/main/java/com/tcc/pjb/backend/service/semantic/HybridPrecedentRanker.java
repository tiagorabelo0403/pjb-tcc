package com.tcc.pjb.backend.service.semantic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.platform.text.JaccardScorer;

@Component
public class HybridPrecedentRanker {

    private final JaccardScorer jaccardScorer;

    public HybridPrecedentRanker(JaccardScorer jaccardScorer) {
        this.jaccardScorer = jaccardScorer;
    }

    public List<SemanticRankedHit> rank(String query,
                                        List<VectorSearchHit> hits,
                                        List<Precedente> precedentes,
                                        RamoDireito ramo,
                                        RitoProcessual rito,
                                        int topK) {
        Map<Long, Float> vectorScores = new HashMap<>();
        for (VectorSearchHit hit : hits) {
            try {
                vectorScores.put(Long.parseLong(hit.id()), hit.score());
            } catch (NumberFormatException ignored) {
            }
        }

        List<SemanticRankedHit> ranked = new ArrayList<>(precedentes.size());
        for (Precedente precedente : precedentes) {
            double lexical = jaccardScorer.score(query, textualCorpus(precedente));
            double contextual = contextualScore(precedente, ramo, rito);
            float vector = vectorScores.getOrDefault(precedente.getId(), 0.0f);
            double finalScore = (vector * 0.70d) + (lexical * 0.20d) + (contextual * 0.10d);
            ranked.add(new SemanticRankedHit(precedente, vector, lexical, contextual, finalScore));
        }

        ranked.sort(Comparator.comparingDouble(SemanticRankedHit::finalScore).reversed()
                .thenComparing(hit -> hit.precedente().getId(), Comparator.nullsLast(Long::compareTo)));
        return ranked.size() <= topK ? List.copyOf(ranked) : List.copyOf(ranked.subList(0, topK));
    }

    private String textualCorpus(Precedente precedente) {
        StringBuilder sb = new StringBuilder(256);
        if (precedente.getTitulo() != null) {
            sb.append(precedente.getTitulo()).append('\n');
        }
        if (precedente.getTese() != null) {
            sb.append(precedente.getTese()).append('\n');
        }
        if (precedente.getEmentaResumo() != null) {
            sb.append(precedente.getEmentaResumo());
        }
        return sb.toString();
    }

    private double contextualScore(Precedente precedente, RamoDireito ramo, RitoProcessual rito) {
        double score = 0.0d;
        if (ramo != null && ramo == precedente.getRamoSugerido()) {
            score += 0.6d;
        }
        if (rito != null && rito == precedente.getRitoSugerido()) {
            score += 0.4d;
        }
        return Math.min(1.0d, score);
    }
}
