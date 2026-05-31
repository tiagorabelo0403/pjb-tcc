package com.tcc.pjb.backend.ai.common;

import com.tcc.pjb.backend.core.guard.MockGuardAuditEvent;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentQuery;
import com.tcc.pjb.backend.core.guard.MockGuardEnvironmentValidator;
import com.tcc.pjb.backend.core.guard.MockGuardViolation;
import com.tcc.pjb.backend.core.guard.MockGuardViolationException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
@Profile({"dev", "test"})
@ConditionalOnProperty(prefix = "pjb.ai.vector", name = "mode", havingValue = "mock")
public class VectorSearchServiceMock implements VectorSearchService {

    
    
    
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_MIN_SCORE = 0.0;

    private static final Pattern TOKEN_SPLIT =
            Pattern.compile("[^\\p{L}\\p{Nd}]+");

    private static final Set<String> STOPWORDS = Set.of(
            "de","da","do","das","dos","a","o","as","os",
            "e","em","no","na","nos","nas","por","para","com",
            "art","artigo","lei","cf","cpc","stf","stj"
    );

    
    
    
    private final Map<String, Doc> docs = new ConcurrentHashMap<>();
    private final Map<String, Double> idfCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> usageGlobal = new ConcurrentHashMap<>();
    private final MockGuardEnvironmentQuery mockGuardQuery;
    private final ApplicationEventPublisher eventPublisher;

    public VectorSearchServiceMock(MockGuardEnvironmentQuery mockGuardQuery,
                                   ApplicationEventPublisher eventPublisher) {
        this.mockGuardQuery = Objects.requireNonNull(mockGuardQuery, "mockGuardQuery");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        if (mockGuardQuery.isRealEnvironment()) {
            MockGuardViolation violation = MockGuardViolation.of(
                    "vector-search", MockGuardEnvironmentValidator.VECTOR_MOCK_PROPERTY,
                    mockGuardQuery.activeGuardProfile());
            mockGuardQuery.recordViolation("vector-search");
            eventPublisher.publishEvent(new MockGuardAuditEvent(this, violation));
            throw new MockGuardViolationException(violation);
        }
        seedBase();
        recomputeIdf();
    }

    
    
    

    @Override
    public VectorSearchResult searchSimilarResult(
            String query,
            Map<String, Object> filtros,
            int topK
    ) {
        Objects.requireNonNull(query, "query não pode ser nula");

        int k = topK > 0 ? topK : DEFAULT_TOP_K;
        double minScore = parseDouble(filtros, "minScore", DEFAULT_MIN_SCORE);

        List<String> qTokens = tokenize(query);
        Map<String, Double> qTf = tf(qTokens);
        Map<String, Double> qTfidf = tfidf(qTf);

        List<Ranked> ranked = new ArrayList<>();

        for (Doc d : docs.values()) {
            Map<String, Double> dTf = tf(tokenize(d.content));
            Map<String, Double> dTfidf = tfidf(dTf);

            double cosine = cosine(qTfidf, dTfidf);
            double usageBoost =
                    1.0 + Math.log(1 + usageGlobal
                            .getOrDefault(d.id, new AtomicInteger(0)).get());

            double score = cosine * usageBoost;

            if (score >= minScore) {
                ranked.add(new Ranked(d, score, cosine));
            }
        }

        ranked.sort(Comparator
                .comparingDouble((Ranked r) -> r.score)
                .reversed());

        List<ResultItem> itens = ranked.stream()
                .limit(k)
                .map(r -> {
                    usageGlobal
                            .computeIfAbsent(r.doc.id, x -> new AtomicInteger())
                            .incrementAndGet();

                    return new ResultItem(
                            r.doc.id,
                            r.doc.title,
                            r.doc.branch,
                            round(r.score),
                            round(r.cosine),
                            1.0
                    );
                })
                .toList();

        return new VectorSearchResult(
                query,
                Instant.now(),
                itens,
                Map.of(
                        "tokens", qTokens,
                        "idfTop", topIdf(10)
                ),
                Map.of(
                        "docs", docs.size(),
                        "pipeline", "V1→V2→V3"
                ),
                "mock-v3"
        );
    }

    
    
    

    @Override
    public VectorSearchResult searchSimilarV1(
            String query,
            Map<String, Object> filtros,
            int topK
    ) {
        return searchSimilarResult(query, filtros, topK);
    }

    @Override
    public VectorSearchResult searchSimilarV2(
            String query,
            Map<String, Object> filtros,
            int topK
    ) {
        filtros = filtros != null ? new HashMap<>(filtros) : new HashMap<>();
        filtros.put("minScore", 0.05);
        return searchSimilarResult(query, filtros, topK);
    }

    @Override
    public VectorSearchResult searchSimilarV3(
            String query,
            Map<String, Object> filtros,
            int topK
    ) {
        filtros = filtros != null ? new HashMap<>(filtros) : new HashMap<>();
        filtros.put("minScore", 0.10);
        return searchSimilarResult(query, filtros, topK);
    }

    
    
    

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        String norm = normalize(text);
        return Arrays.stream(TOKEN_SPLIT.split(norm))
                .filter(t -> !t.isBlank())
                .filter(t -> !STOPWORDS.contains(t))
                .toList();
    }

    private String normalize(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replace("á","a").replace("é","e")
                .replace("í","i").replace("ó","o")
                .replace("ú","u").replace("ç","c");
    }

    private Map<String, Double> tf(List<String> tokens) {
        Map<String, Double> f = new HashMap<>();
        for (String t : tokens) f.merge(t, 1.0, Double::sum);
        double size = Math.max(1, tokens.size());
        f.replaceAll((k,v) -> v / size);
        return f;
    }

    private Map<String, Double> tfidf(Map<String, Double> tf) {
        Map<String, Double> out = new HashMap<>();
        for (var e : tf.entrySet()) {
            out.put(e.getKey(),
                    e.getValue() * idfCache.getOrDefault(e.getKey(), 0.0));
        }
        return out;
    }

    private double cosine(
            Map<String, Double> v1,
            Map<String, Double> v2
    ) {
        double dot = 0, n1 = 0, n2 = 0;
        for (var e : v1.entrySet()) {
            n1 += e.getValue() * e.getValue();
            Double v = v2.get(e.getKey());
            if (v != null) dot += e.getValue() * v;
        }
        for (double v : v2.values()) n2 += v * v;
        double d = Math.sqrt(n1) * Math.sqrt(n2);
        return d == 0 ? 0 : dot / d;
    }

    private void recomputeIdf() {
        int N = Math.max(1, docs.size());
        Map<String, Integer> df = new HashMap<>();

        for (Doc d : docs.values()) {
            new HashSet<>(tokenize(d.content))
                    .forEach(t -> df.merge(t, 1, Integer::sum));
        }

        idfCache.clear();
        for (var e : df.entrySet()) {
            idfCache.put(
                    e.getKey(),
                    Math.log((N + 1.0) / (e.getValue() + 1.0)) + 1
            );
        }
    }

    private List<Map.Entry<String, Double>> topIdf(int n) {
        return idfCache.entrySet().stream()
                .sorted(Map.Entry.<String,Double>comparingByValue().reversed())
                .limit(n)
                .toList();
    }

    private double parseDouble(
            Map<String, Object> m,
            String k,
            double d
    ) {
        if (m == null) return d;
        Object o = m.get(k);
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof String s) {
            try { return Double.parseDouble(s); } catch (Exception ignored) {}
        }
        return d;
    }

    private double round(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }

    
    
    

    private void seedBase() {
        add("cpc-319","CPC 319","Requisitos da petição inicial","Processual Civil");
        add("cpc-300","CPC 300","Tutela de urgência","Processual Civil");
        add("cf-5","CF Art 5","Direitos fundamentais","Constitucional");
    }

    private void add(String id,String t,String c,String r) {
        docs.put(id, new Doc(id,t,c,r));
    }

    
    
    

    private record Doc(String id,String title,String content,String branch){}
    private record Ranked(Doc doc,double score,double cosine){}
}
