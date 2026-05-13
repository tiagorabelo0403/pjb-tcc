package com.tcc.pjb.backend.ai.juridica.pipeline;

import com.tcc.pjb.backend.ai.common.VectorSearchService;
import com.tcc.pjb.backend.ai.core.model.AgentExecutionContext;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class JuridicaSemanticSourceDistillationService {

    private static final Pattern SPLIT = Pattern.compile("[^\\p{L}\\p{Nd}]+");
    private static final Set<String> STOP = Set.of(
            "de", "da", "do", "das", "dos", "a", "o", "e", "em", "para", "por", "com",
            "art", "artigo", "lei", "processo", "acao", "ação", "peticao", "petição"
    );

    public DistillationResult distill(AgentExecutionContext ctx,
                                      String query,
                                      VectorSearchService.VectorSearchResult raw,
                                      int evidenceBudget) {
        Objects.requireNonNull(ctx, "ctx");
        if (raw == null || raw.resultados() == null || raw.resultados().isEmpty()) {
            return DistillationResult.empty(query, evidenceBudget);
        }

        Map<String, Object> payload = ctx.request().getPayload();
        String ramo = firstString(payload, "ramoDireito", "ramo", "resolvedRamoDireito");
        String materia = firstString(payload, "materiaPrincipal", "materia", "resolvedMateriaPrincipal");
        String procedureFamily = firstString(payload, "resolvedProcedureFamily", "procedureFamily");
        String capability = ctx.capability() == null ? "" : ctx.capability();
        String profile = resolveDistillationProfile(payload);
        int budget = Math.max(1, evidenceBudget);
        List<String> queryTokens = tokens(query);

        ArrayList<DistilledCandidate> candidates = new ArrayList<>();
        for (VectorSearchService.ResultItem item : raw.resultados()) {
            if (item == null) continue;
            DistilledCandidate candidate = buildCandidate(item, queryTokens, ramo, materia, procedureFamily, capability, raw.iaVersion(), ctx.now(), profile);
            if (candidate == null) continue;
            candidates.add(candidate);
        }

        candidates.sort(Comparator.comparingDouble(DistilledCandidate::weightedScore).reversed());
        LinkedHashMap<String, DistilledCandidate> deduped = new LinkedHashMap<>();
        for (DistilledCandidate candidate : candidates) {
            String key = dedupeKey(candidate.evidence());
            DistilledCandidate previous = deduped.get(key);
            if (previous == null || candidate.weightedScore() > previous.weightedScore()) {
                deduped.put(key, candidate);
            }
        }

        ArrayList<EvidenceItem> selected = new ArrayList<>();
        ArrayList<Map<String, Object>> audits = new ArrayList<>();
        LinkedHashSet<String> expansions = new LinkedHashSet<>();
        int discarded = 0;
        for (DistilledCandidate candidate : deduped.values()) {
            if (selected.size() >= budget) {
                discarded++;
                continue;
            }
            selected.add(candidate.evidence());
            audits.add(candidate.audit());
            expansions.addAll(candidate.expansionSeeds());
        }

        double mean = selected.stream()
                .map(EvidenceItem::getScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("profile", profile);
        meta.put("rawCount", raw.resultados().size());
        meta.put("candidateCount", candidates.size());
        meta.put("selectedCount", selected.size());
        meta.put("discardedCount", discarded);
        meta.put("weightedMean", round(mean));
        meta.put("sourceFamilies", summarizeFamilies(selected));
        meta.put("expansionSeeds", List.copyOf(expansions).subList(0, Math.min(expansions.size(), 10)));
        meta.put("audits", List.copyOf(audits));

        return new DistillationResult(List.copyOf(selected), Map.copyOf(meta));
    }

    private DistilledCandidate buildCandidate(VectorSearchService.ResultItem item,
                                              List<String> queryTokens,
                                              String ramo,
                                              String materia,
                                              String procedureFamily,
                                              String capability,
                                              String iaVersion,
                                              Instant now,
                                              String profile) {
        String title = sanitize(item.titulo());
        String branch = sanitize(item.ramo());
        List<String> titleTokens = tokens(title + " " + branch);
        double coverage = coverage(queryTokens, titleTokens);
        double authorityWeight = authorityWeight(title, branch);
        double branchAlignment = branchAlignment(ramo, materia, procedureFamily, title, branch);
        double capabilityAlignment = capabilityAlignment(capability, title, procedureFamily);
        double baseScore = clamp(item.score());
        double cosine = clamp(item.cosine());
        double boost = clamp(item.boost());
        double profileBonus = profile.contains("STRICT") ? 0.06 : 0.02;
        double weighted = 0.34 * baseScore
                + 0.24 * cosine
                + 0.10 * boost
                + 0.12 * coverage
                + 0.10 * authorityWeight
                + 0.06 * branchAlignment
                + 0.04 * capabilityAlignment
                + profileBonus;

        if (weighted <= 0.05) {
            return null;
        }

        EvidenceItem.EvidenceType type = classify(title, branch, procedureFamily);
        EvidenceItem evidence = EvidenceItem.builder()
                .docId(sanitize(item.docId()))
                .tipo(type)
                .titulo(title)
                .tribunal(branch)
                .orgaoJulgador(null)
                .fonteSistema("VectorSearch/" + sanitize(iaVersion))
                .url(null)
                .dataPublicacao(now)
                .score(round(weighted))
                .trecho(buildTrace(baseScore, cosine, coverage, authorityWeight, branchAlignment, capabilityAlignment, profile))
                .build();

        LinkedHashMap<String, Object> audit = new LinkedHashMap<>();
        audit.put("docId", evidence.getDocId());
        audit.put("title", evidence.getTitulo());
        audit.put("type", evidence.getTipo().name());
        audit.put("weightedScore", evidence.getScore());
        audit.put("coverage", round(coverage));
        audit.put("authorityWeight", round(authorityWeight));
        audit.put("branchAlignment", round(branchAlignment));
        audit.put("capabilityAlignment", round(capabilityAlignment));
        audit.put("profile", profile);

        LinkedHashSet<String> expansions = new LinkedHashSet<>();
        for (String token : titleTokens) {
            if (STOP.contains(token) || queryTokens.contains(token)) continue;
            expansions.add(token);
            if (expansions.size() >= 4) break;
        }

        return new DistilledCandidate(evidence, round(weighted), Map.copyOf(audit), List.copyOf(expansions));
    }

    private String resolveDistillationProfile(Map<String, Object> payload) {
        Object governance = payload.get("meshGovernance");
        if (governance instanceof Map<?, ?> map) {
            Object rag = map.get("rag");
            if (rag instanceof Map<?, ?> ragMap) {
                Object profile = ragMap.get("distillationProfile");
                if (profile != null) return sanitize(String.valueOf(profile));
            }
        }
        Object direct = payload.get("ssdProfile");
        if (direct != null) return sanitize(String.valueOf(direct));
        return "SEMANTIC_SOURCE_DISTILLATION_BALANCED_V2";
    }

    private Map<String, Integer> summarizeFamilies(Collection<EvidenceItem> evidences) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        if (evidences == null) return Map.of();
        for (EvidenceItem evidence : evidences) {
            if (evidence == null || evidence.getTipo() == null) continue;
            out.merge(evidence.getTipo().name(), 1, Integer::sum);
        }
        return Collections.unmodifiableMap(out);
    }

    private EvidenceItem.EvidenceType classify(String title, String branch, String procedureFamily) {
        String corpus = (title + " " + branch + " " + procedureFamily).toUpperCase(Locale.ROOT);
        if (containsAny(corpus, "LEI", "DECRETO", "RESOLUCAO", "RESOLUÇÃO", "PORTARIA", "CF/88", "CONSTITUICAO", "CONSTITUIÇÃO")) {
            return EvidenceItem.EvidenceType.LEGISLACAO;
        }
        if (containsAny(corpus, "EDITAL", "DIARIO", "DIÁRIO", "PUBLICACAO", "PUBLICAÇÃO")) {
            return EvidenceItem.EvidenceType.DIARIO_OFICIAL;
        }
        if (containsAny(corpus, "PETICAO", "PETIÇÃO", "CONTESTACAO", "CONTESTAÇÃO", "RECURSO", "INICIAL")) {
            return EvidenceItem.EvidenceType.PECA_PROCESSUAL;
        }
        if (containsAny(corpus, "ARTIGO", "DOUTRINA", "MANUAL", "TRATADO")) {
            return EvidenceItem.EvidenceType.DOUTRINA;
        }
        return EvidenceItem.EvidenceType.JURISPRUDENCIA;
    }

    private double branchAlignment(String ramo,
                                   String materia,
                                   String procedureFamily,
                                   String title,
                                   String branch) {
        double weight = 0.0;
        String corpus = (title + " " + branch).toUpperCase(Locale.ROOT);
        if (matchesAny(corpus, ramo)) weight += 0.45;
        if (matchesAny(corpus, materia)) weight += 0.30;
        if (matchesAny(corpus, procedureFamily)) weight += 0.25;
        return Math.min(1.0, weight);
    }

    private double capabilityAlignment(String capability, String title, String procedureFamily) {
        if (capability == null || capability.isBlank()) return 0.0;
        String corpus = (title + " " + procedureFamily).toUpperCase(Locale.ROOT);
        if (capability.contains("PETICAO") && containsAny(corpus, "PETI", "INICIAL", "PEDIDO", "REQUER")) return 1.0;
        if (capability.contains("SENTENCA") && containsAny(corpus, "SENTEN", "ACORDAO", "ACÓRDÃO", "JULGAMENTO")) return 1.0;
        if (capability.contains("COMPETENCIA") && containsAny(corpus, "COMPETEN", "FORO", "SEÇÃO", "SECAO", "COMARCA")) return 1.0;
        return 0.35;
    }

    private double authorityWeight(String title, String branch) {
        String corpus = (title + " " + branch).toUpperCase(Locale.ROOT);
        if (containsAny(corpus, "STF", "SUPREMO")) return 1.0;
        if (containsAny(corpus, "STJ", "TST", "TSE", "STM")) return 0.92;
        if (containsAny(corpus, "TRF", "TJ", "TRE", "TRT")) return 0.82;
        if (containsAny(corpus, "CNJ", "AGU", "MPF")) return 0.76;
        return 0.55;
    }

    private double coverage(List<String> queryTokens, List<String> titleTokens) {
        if (queryTokens == null || queryTokens.isEmpty() || titleTokens == null || titleTokens.isEmpty()) return 0.0;
        int matches = 0;
        for (String token : queryTokens) {
            if (titleTokens.contains(token)) matches++;
        }
        return Math.min(1.0, (double) matches / (double) queryTokens.size());
    }

    private List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = sanitize(text).toLowerCase(Locale.ROOT);
        ArrayList<String> out = new ArrayList<>();
        for (String token : SPLIT.split(normalized)) {
            if (token == null || token.isBlank()) continue;
            if (STOP.contains(token)) continue;
            out.add(token);
            if (out.size() >= 32) break;
        }
        return List.copyOf(out);
    }

    private String buildTrace(double baseScore,
                              double cosine,
                              double coverage,
                              double authorityWeight,
                              double branchAlignment,
                              double capabilityAlignment,
                              String profile) {
        return "profile=" + profile
                + " base=" + round(baseScore)
                + " cosine=" + round(cosine)
                + " coverage=" + round(coverage)
                + " authority=" + round(authorityWeight)
                + " ramo=" + round(branchAlignment)
                + " capability=" + round(capabilityAlignment);
    }

    private String dedupeKey(EvidenceItem evidence) {
        if (evidence == null) return "NULL";
        String id = sanitize(evidence.getDocId());
        if (!id.isBlank()) return id;
        return sanitize(evidence.getTitulo()) + "|" + sanitize(evidence.getTribunal());
    }

    private String firstString(Map<String, Object> payload, String... keys) {
        if (payload == null || payload.isEmpty()) return "";
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) continue;
            String text = sanitize(String.valueOf(value));
            if (!text.isBlank()) return text;
        }
        return "";
    }

    private boolean matchesAny(String corpus, String raw) {
        if (raw == null || raw.isBlank()) return false;
        String normalized = raw.toUpperCase(Locale.ROOT).replace('_', ' ');
        for (String token : SPLIT.split(normalized)) {
            if (token == null || token.isBlank()) continue;
            if (token.length() < 3) continue;
            if (corpus.contains(token)) return true;
        }
        return false;
    }

    private boolean containsAny(String corpus, String... tokens) {
        if (corpus == null || corpus.isBlank()) return false;
        for (String token : tokens) {
            if (token != null && !token.isBlank() && corpus.contains(token.toUpperCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private String sanitize(String raw) {
        if (raw == null) return "";
        String value = raw.replace('\u0000', ' ').trim();
        if (value.length() > 220) value = value.substring(0, 220);
        return value;
    }

    private double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    private double round(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    private record DistilledCandidate(
            EvidenceItem evidence,
            double weightedScore,
            Map<String, Object> audit,
            List<String> expansionSeeds
    ) {
    }

    public record DistillationResult(
            List<EvidenceItem> evidences,
            Map<String, Object> metadata
    ) {
        public DistillationResult {
            evidences = evidences == null ? List.of() : List.copyOf(evidences);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
        }

        public static DistillationResult empty(String query, int budget) {
            return new DistillationResult(List.of(), Map.of(
                    "profile", "SEMANTIC_SOURCE_DISTILLATION_EMPTY",
                    "query", query == null ? "" : query,
                    "evidenceBudget", Math.max(1, budget),
                    "selectedCount", 0
            ));
        }
    }
}
