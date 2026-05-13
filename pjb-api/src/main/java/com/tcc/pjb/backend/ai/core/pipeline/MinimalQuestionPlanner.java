package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.platform.observability.ai.AiTelemetryDomain;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import lombok.RequiredArgsConstructor;

@RefreshScope
@Component
@RequiredArgsConstructor
public class MinimalQuestionPlanner {

    private final AiQuestionPlannerProperties props;

    public List<String> topQuestions(AiTelemetryDomain domain,
                                    String capability,
                                    ApiVersion version,
                                    List<String> candidates) {
        AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
        ApiVersion v = (version != null) ? version : ApiVersion.latest();

        List<String> in = (candidates == null) ? List.of() : candidates;

        
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        for (String c : in) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isBlank()) continue;
            uniq.add(t);
        }
        List<String> pool = new ArrayList<>(uniq);
        if (pool.isEmpty()) return List.of();

        AiQuestionPlannerProperties.DomainRules rules = props.getDomains().get(d.tag());
        int max = (rules != null && rules.getMaxQuestions() > 0) ? rules.getMaxQuestions() : 5;
        double noveltyBonus = (rules != null) ? rules.getNoveltyBonus() : 0.75;

        Map<String, Double> weights = (rules != null) ? rules.getCategoryWeights() : Map.of();
        Map<String, List<String>> kw = (rules != null) ? rules.getCategoryKeywords() : Map.of();

        
        if (kw == null || kw.isEmpty()) {
            return pool.subList(0, Math.min(max, pool.size()));
        }

        Map<String, Set<String>> qCats = new LinkedHashMap<>();
        for (String q : pool) {
            qCats.put(q, classifyCategories(q, kw));
        }

        List<String> picked = new ArrayList<>();
        Set<String> covered = new HashSet<>();

        for (int i = 0; i < Math.min(max, pool.size()); i++) {
            String bestQ = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (String q : pool) {
                if (picked.contains(q)) continue;
                Set<String> cats = qCats.getOrDefault(q, Set.of());
                double base = 1.0;
                for (String c : cats) {
                    base += weights.getOrDefault(c, 1.0);
                }

                int newCats = 0;
                for (String c : cats) {
                    if (!covered.contains(c)) newCats++;
                }

                double score = base + (newCats * noveltyBonus);

                
                if (v.isAtLeast(ApiVersion.V3) && looksLikeSpecific(q)) score += 0.25;

                if (score > bestScore) {
                    bestScore = score;
                    bestQ = q;
                }
            }

            if (bestQ == null) break;
            picked.add(bestQ);
            covered.addAll(qCats.getOrDefault(bestQ, Set.of()));
        }

        return List.copyOf(picked);
    }

    private static Set<String> classifyCategories(String q, Map<String, List<String>> categoryKeywords) {
        String s = q == null ? "" : q.toLowerCase(Locale.ROOT);
        Set<String> cats = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> e : categoryKeywords.entrySet()) {
            String cat = e.getKey();
            if (cat == null || cat.isBlank()) continue;
            List<String> kws = e.getValue();
            if (kws == null || kws.isEmpty()) continue;
            for (String k : kws) {
                if (k == null || k.isBlank()) continue;
                if (s.contains(k.toLowerCase(Locale.ROOT))) {
                    cats.add(cat.trim().toUpperCase(Locale.ROOT));
                    break;
                }
            }
        }
        if (cats.isEmpty()) cats.add("OTHER");
        return cats;
    }

    private static boolean looksLikeSpecific(String q) {
        if (q == null) return false;
        String s = q.toLowerCase(Locale.ROOT);
        return s.contains("data") || s.contains("ano") || s.contains("valor") || s.contains("document") || s.contains("exame") || s.contains("tribunal");
    }
}
