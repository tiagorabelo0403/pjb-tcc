package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ProceduralExecutiveExplainabilityService {

    private ProceduralExecutiveExplainabilityService() {
    }

    public static ProceduralExecutiveExplainabilityReport analyze(Map<String, Object> payload,
                                                                 String actionNature,
                                                                 String actionFamily,
                                                                 String riskLevel,
                                                                 ProceduralIntelligenceAdvisoryReport advisory,
                                                                 ProceduralDecisionQualityReport quality,
                                                                 ProceduralAutomationPolicyReport policy) {
        List<ProceduralExecutiveExplanationItem> items = new ArrayList<>();
        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        LinkedHashSet<String> legalAnchors = new LinkedHashSet<>();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        String corpus = buildCorpus(payload, actionNature, actionFamily, advisory);
        RamoDireito ramo = advisory != null ? advisory.suggestedRamo() : null;
        RitoProcessual rito = advisory != null ? advisory.suggestedRito() : null;

        if (quality != null && quality.convergenceScore() >= 0.84d && quality.conflicts().isEmpty()) {
            items.add(item(ProceduralExecutiveExplanationCode.AXIS_CONSENSUS_HIGH, "INFO", false,
                    "Convergência=" + round(quality.convergenceScore()) + " determinismo=" + round(quality.determinismScore())));
            highlights.add("Eixos processuais convergem com boa estabilidade semântica.");
        }
        if (quality != null && (!quality.conflicts().isEmpty() || quality.reviewPressureScore() >= 0.45d)) {
            items.add(item(ProceduralExecutiveExplanationCode.AXIS_CONFLICT_REVIEW, "WARN", true,
                    firstNonBlank(join(quality.conflicts(), 3), quality.operatingModeHint())));
            highlights.add("Há tensão classificatória e revisão humana permanece indicada.");
        }
        if (advisory != null && advisory.naturezaPrincipal() != null) {
            items.add(item(ProceduralExecutiveExplanationCode.NATUREZA_JURIDICA_SIGNAL, "INFO", false,
                    advisory.naturezaPrincipal().name()));
            highlights.add("Natureza jurídica predominante: " + advisory.naturezaPrincipal().label() + '.');
        }
        if (quality != null && quality.evidenceScore() >= 0.72d) {
            items.add(item(ProceduralExecutiveExplanationCode.QUALITY_STRONG_EVIDENCE, "INFO", false,
                    "evidence=" + round(quality.evidenceScore())));
        }
        if (quality != null && quality.determinismScore() < 0.57d) {
            items.add(item(ProceduralExecutiveExplanationCode.QUALITY_LOW_DETERMINISM, "WARN", true,
                    "determinism=" + round(quality.determinismScore())));
        }
        if (policy != null) {
            switch (policy.mode()) {
                case AUTOMATE_SAFE -> items.add(item(ProceduralExecutiveExplanationCode.AUTOMATION_SAFE_ROUTE, "INFO", false,
                        policy.autoRouteEligible() ? "roteamento elegível" : "somente preparação segura"));
                case ASSISTED_DECISION -> items.add(item(ProceduralExecutiveExplanationCode.AUTOMATION_ASSISTED_ONLY, "INFO", false,
                        policy.domain().label()));
                case HUMAN_GATE_REQUIRED, ADVISORY_ONLY -> items.add(item(ProceduralExecutiveExplanationCode.AUTOMATION_HUMAN_GATE, "WARN", true,
                        policy.domain().label()));
            }
            if (isSensitive(policy.domain())) {
                items.add(item(ProceduralExecutiveExplanationCode.SENSITIVE_DOMAIN_RESTRICTION, "WARN", true, policy.domain().label()));
                highlights.add("Domínio sensível reduz a latitude de automação.");
            }
        }
        if (advisory != null && !advisory.riskFlags().isEmpty()) {
            items.add(item(ProceduralExecutiveExplanationCode.MISSING_FOUNDATIONAL_SIGNAL, "WARN", true,
                    join(advisory.riskFlags(), 3)));
        }

        if (containsAny(corpus, "ACORDO", "TRANSACAO", "HOMOLOGACAO DE ACORDO", "CEJUSC", "CONCILIACAO EXITOSA")) {
            items.add(item(ProceduralExecutiveExplanationCode.JUDICIAL_DRAFT_AGREEMENT_TEMPLATE, "INFO", false, "homologação negocial"));
            legalAnchors.add("CPC art. 487, III, b");
            legalAnchors.add("CPC art. 489");
        }
        if (containsAny(corpus, "DESISTENCIA", "DESISTE DA ACAO", "DESISTIU DA ACAO", "EXTINCAO SEM RESOLUCAO DO MERITO")) {
            items.add(item(ProceduralExecutiveExplanationCode.JUDICIAL_DRAFT_DESISTENCE_TEMPLATE, "INFO", false, "desistência homologável"));
            legalAnchors.add("CPC art. 485, VIII, §§ 4º e 5º");
            legalAnchors.add("CPC art. 489");
        }

        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.ELEITORAL || ramo == RamoDireito.MILITAR || rito != null && rito.isPenal()) {
            legalAnchors.add("Revisão humana obrigatória para atos jurisdicionais terminais em domínio sensível");
        }

        metadata.put("riskLevel", riskLevel);
        metadata.put("operatingModeHint", quality != null ? quality.operatingModeHint() : null);
        metadata.put("automationMode", policy != null && policy.mode() != null ? policy.mode().name() : null);
        metadata.put("automationDomain", policy != null && policy.domain() != null ? policy.domain().name() : null);
        metadata.put("naturezaJuridicaCanonical", advisory != null && advisory.naturezaPrincipal() != null ? advisory.naturezaPrincipal().name() : null);
        metadata.put("itemCount", items.size());
        metadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);

        String actionFrame = resolveActionFrame(policy, quality);
        String summary = resolveSummary(items, actionFrame, advisory, quality, policy);
        return new ProceduralExecutiveExplainabilityReport(
                summary,
                actionFrame,
                List.copyOf(items),
                List.copyOf(highlights),
                List.copyOf(legalAnchors),
                Collections.unmodifiableMap(metadata)
        );
    }

    private static ProceduralExecutiveExplanationItem item(ProceduralExecutiveExplanationCode code,
                                                           String severity,
                                                           boolean actionRequired,
                                                           String detail) {
        return new ProceduralExecutiveExplanationItem(code, severity, actionRequired,
                ProceduralExecutiveExplanationMessages.resolve(code, detail), detail);
    }

    private static String resolveActionFrame(ProceduralAutomationPolicyReport policy,
                                             ProceduralDecisionQualityReport quality) {
        if (policy != null) {
            return switch (policy.mode()) {
                case AUTOMATE_SAFE -> "AUTOMACAO_PREPARATORIA_SEGURA";
                case ASSISTED_DECISION -> "DECISAO_ASSISTIDA";
                case HUMAN_GATE_REQUIRED -> "GATE_HUMANO_OBRIGATORIO";
                case ADVISORY_ONLY -> "SOMENTE_ADVISORY";
            };
        }
        if (quality != null && quality.safeAutomationEligible()) {
            return "AUTOMACAO_PREPARATORIA_SEGURA";
        }
        return "DECISAO_ASSISTIDA";
    }

    private static String resolveSummary(List<ProceduralExecutiveExplanationItem> items,
                                         String actionFrame,
                                         ProceduralIntelligenceAdvisoryReport advisory,
                                         ProceduralDecisionQualityReport quality,
                                         ProceduralAutomationPolicyReport policy) {
        List<String> parts = new ArrayList<>();
        parts.add("Frame=" + actionFrame);
        if (advisory != null && advisory.naturezaPrincipal() != null) {
            parts.add("natureza=" + advisory.naturezaPrincipal().label());
        }
        if (policy != null && policy.domain() != null) {
            parts.add("domínio=" + policy.domain().label());
        }
        if (quality != null) {
            parts.add("convergência=" + round(quality.convergenceScore()));
            parts.add("determinismo=" + round(quality.determinismScore()));
        }
        if (!items.isEmpty()) {
            parts.add("sinais=" + items.size());
        }
        return String.join(" | ", parts);
    }

    private static String buildCorpus(Map<String, Object> payload,
                                      String actionNature,
                                      String actionFamily,
                                      ProceduralIntelligenceAdvisoryReport advisory) {
        StringBuilder sb = new StringBuilder();
        append(sb, actionNature);
        append(sb, actionFamily);
        if (advisory != null) {
            append(sb, advisory.primaryReason());
            advisory.supportingSignals().forEach(v -> append(sb, v));
            advisory.discardedAlternatives().forEach(v -> append(sb, v));
            advisory.recommendedDocuments().forEach(v -> append(sb, v));
            advisory.riskFlags().forEach(v -> append(sb, v));
        }
        if (payload != null) {
            payload.values().forEach(v -> append(sb, flatten(v)));
        }
        return normalize(sb.toString());
    }

    private static String flatten(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream().map(ProceduralExecutiveExplainabilityService::flatten).filter(Objects::nonNull).reduce((a, b) -> a + ' ' + b).orElse(null);
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder sb = new StringBuilder();
            for (Object item : iterable) {
                append(sb, flatten(item));
            }
            return sb.isEmpty() ? null : sb.toString();
        }
        return String.valueOf(value);
    }

    private static void append(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(value.trim());
    }

    private static boolean containsAny(String corpus, String... needles) {
        if (corpus == null || corpus.isBlank() || needles == null) {
            return false;
        }
        String normalized = normalize(corpus);
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && normalized.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private static String join(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).limit(limit).reduce((a, b) -> a + "; " + b).orElse(null);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isSensitive(ProceduralAutomationDomain domain) {
        if (domain == null) {
            return false;
        }
        return switch (domain) {
            case FAMILY_AND_SUCCESSIONS,
                    PENAL_SENSITIVE,
                    ELECTORAL_SENSITIVE,
                    MILITARY_SENSITIVE,
                    INTERNATIONAL_COOPERATION,
                    HIGH_SECRECY,
                    COLLECTIVE_STRUCTURAL,
                    CONSTITUTIONAL_MANDAMENTAL -> true;
            default -> false;
        };
    }

    private static String round(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
