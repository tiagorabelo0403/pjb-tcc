package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ProceduralDecisionQualityEngine {

    private ProceduralDecisionQualityEngine() {
    }

    public static ProceduralDecisionQualityReport analyze(Map<String, Object> payload,
                                                          Map<String, Object> canonicalMetadata,
                                                          String actionNature,
                                                          String actionFamily,
                                                          TipoJustica routingTipoJustica,
                                                          String routingRito,
                                                          String riskLevel,
                                                          double routingConfidence,
                                                          ProceduralIntelligenceAdvisoryReport advisory) {
        LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        if (payload != null) {
            source.putAll(payload);
        }
        if (canonicalMetadata != null) {
            canonicalMetadata.forEach((key, value) -> source.putIfAbsent("canonical:" + key, value));
        }
        source.put("__routingActionNature", actionNature);
        source.put("__routingActionFamily", actionFamily);
        source.put("__routingTipoJustica", routingTipoJustica != null ? routingTipoJustica.name() : null);
        source.put("__routingRito", routingRito);
        source.put("__riskLevel", riskLevel);
        source.put("__routingConfidence", routingConfidence);
        if (advisory != null) {
            source.put("__advisoryNatureza", advisory.naturezaPrincipal() != null ? advisory.naturezaPrincipal().name() : null);
            source.put("__advisoryTipoJustica", advisory.suggestedTipoJustica() != null ? advisory.suggestedTipoJustica().name() : null);
            source.put("__advisoryRamo", advisory.suggestedRamo() != null ? advisory.suggestedRamo().name() : null);
            source.put("__advisoryRito", advisory.suggestedRito() != null ? advisory.suggestedRito().name() : null);
            source.put("__advisoryMateria", advisory.suggestedMateria() != null ? advisory.suggestedMateria().name() : null);
            source.put("__advisoryConfidence", advisory.confidence());
        }

        AxisVector vector = buildAxisVector(source, advisory);
        List<ProceduralInferenceSignal> signals = collectSignals(source, vector, advisory);
        List<String> conflicts = collectConflicts(vector, advisory, signals);
        List<String> consensus = collectConsensus(vector, advisory);
        List<String> strongSignals = signals.stream()
                .filter(signal -> signal.weight() >= 0.85d && !isConflict(signal))
                .map(ProceduralInferenceSignal::summary)
                .distinct()
                .limit(8)
                .toList();
        List<String> weakSignals = signals.stream()
                .filter(signal -> signal.weight() < 0.85d && !isConflict(signal))
                .map(ProceduralInferenceSignal::summary)
                .distinct()
                .limit(8)
                .toList();
        List<String> riskSignals = new ArrayList<>();
        if (advisory != null) {
            riskSignals.addAll(advisory.riskFlags());
        }
        riskSignals.addAll(signals.stream()
                .filter(ProceduralDecisionQualityEngine::isConflict)
                .map(ProceduralInferenceSignal::summary)
                .toList());
        riskSignals = riskSignals.stream().filter(Objects::nonNull).distinct().toList();

        double convergenceScore = clamp01(computeConvergence(vector, conflicts, advisory));
        double evidenceScore = clamp01(computeEvidenceScore(signals, consensus, advisory));
        double reviewPressureScore = clamp01(computeReviewPressure(conflicts, riskSignals, advisory, source));
        double determinismScore = clamp01((convergenceScore * 0.42d) + (evidenceScore * 0.38d) + ((1d - reviewPressureScore) * 0.20d));
        boolean safeAutomationEligible = determinismScore >= 0.77d
                && reviewPressureScore <= 0.38d
                && conflicts.size() <= 1
                && (advisory == null || !advisory.reviewRequired());
        String operatingModeHint = resolveOperatingMode(safeAutomationEligible, determinismScore, reviewPressureScore, advisory);

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("signalCount", signals.size());
        metadata.put("criticalSignalCount", signals.stream().filter(ProceduralInferenceSignal::critical).count());
        metadata.put("textualSignalCount", signals.stream().filter(signal -> signal instanceof ProceduralInferenceSignal.TextualSignal).count());
        metadata.put("structuredSignalCount", signals.stream().filter(signal -> signal instanceof ProceduralInferenceSignal.StructuredSignal).count());
        metadata.put("conflictSignalCount", signals.stream().filter(ProceduralDecisionQualityEngine::isConflict).count());
        metadata.put("confidenceBand", confidenceBand(Math.max(decimal(source.get("__routingConfidence")), advisory != null ? advisory.confidence() : 0d)));
        metadata.put("axisVector", vector.toMap());
        metadata.put("signals", signals.stream().map(ProceduralInferenceSignal::toMap).toList());
        metadata.put("corpusFingerprint", Integer.toHexString(buildCorpus(source).hashCode()));
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new ProceduralDecisionQualityReport(
                Instant.now(),
                convergenceScore,
                evidenceScore,
                reviewPressureScore,
                determinismScore,
                safeAutomationEligible,
                operatingModeHint,
                consensus,
                conflicts,
                strongSignals,
                weakSignals,
                riskSignals,
                Collections.unmodifiableMap(metadata)
        );
    }

    private static AxisVector buildAxisVector(Map<String, Object> source, ProceduralIntelligenceAdvisoryReport advisory) {
        TipoJustica explicitTipo = TipoJustica.fromString(firstNonBlank(text(source.get("tipoJustica")), text(source.get("canonical:tipoJustica"))));
        TipoJustica routingTipo = TipoJustica.fromString(text(source.get("__routingTipoJustica")));
        TipoJustica advisoryTipo = advisory != null ? advisory.suggestedTipoJustica() : null;
        RamoDireito explicitRamo = RamoDireito.fromString(firstNonBlank(text(source.get("ramoDireito")), text(source.get("canonical:ramoDireito"))));
        RamoDireito advisoryRamo = advisory != null ? advisory.suggestedRamo() : null;
        RitoProcessual explicitRito = RitoProcessual.fromString(firstNonBlank(text(source.get("rito")), text(source.get("canonical:rito"))));
        RitoProcessual routingRito = RitoProcessual.fromString(text(source.get("__routingRito")));
        RitoProcessual advisoryRito = advisory != null ? advisory.suggestedRito() : null;
        MateriaJurisdicao explicitMateria = MateriaJurisdicao.fromString(firstNonBlank(text(source.get("materia")), text(source.get("canonical:materia"))));
        MateriaJurisdicao advisoryMateria = advisory != null ? advisory.suggestedMateria() : null;
        NaturezaJuridicaCanonical advisoryNatureza = advisory != null ? advisory.naturezaPrincipal() : null;
        String actionNature = firstNonBlank(text(source.get("__routingActionNature")), text(source.get("actionNature")));
        String actionFamily = firstNonBlank(text(source.get("__routingActionFamily")), text(source.get("actionFamily")));
        return new AxisVector(explicitTipo, routingTipo, advisoryTipo, explicitRamo, advisoryRamo, explicitRito, routingRito, advisoryRito, explicitMateria, advisoryMateria, advisoryNatureza, actionNature, actionFamily);
    }

    private static List<ProceduralInferenceSignal> collectSignals(Map<String, Object> source, AxisVector vector, ProceduralIntelligenceAdvisoryReport advisory) {
        ArrayList<ProceduralInferenceSignal> signals = new ArrayList<>();
        String corpus = buildCorpus(source);
        appendStructuredSignal(signals, "EXPLICIT_TIPO_JUSTICA", "REQUEST", 0.95d, true, "Tipo de justiça explícito informado", "tipoJustica", vector.explicitTipo() != null ? vector.explicitTipo().name() : null);
        appendStructuredSignal(signals, "ROUTING_TIPO_JUSTICA", "ROUTING", 0.84d, false, "Roteamento sugeriu tipo de justiça", "tipoJustica", vector.routingTipo() != null ? vector.routingTipo().name() : null);
        appendStructuredSignal(signals, "ADVISORY_TIPO_JUSTICA", "ADVISORY", 0.82d, false, "Camada advisory sugeriu tipo de justiça", "tipoJustica", vector.advisoryTipo() != null ? vector.advisoryTipo().name() : null);
        appendStructuredSignal(signals, "EXPLICIT_RAMO", "REQUEST", 0.95d, true, "Ramo jurídico explícito informado", "ramoDireito", vector.explicitRamo() != null ? vector.explicitRamo().name() : null);
        appendStructuredSignal(signals, "ADVISORY_RAMO", "ADVISORY", 0.82d, false, "Camada advisory sugeriu ramo jurídico", "ramoDireito", vector.advisoryRamo() != null ? vector.advisoryRamo().name() : null);
        appendStructuredSignal(signals, "EXPLICIT_RITO", "REQUEST", 0.94d, true, "Rito explícito informado", "rito", vector.explicitRito() != null ? vector.explicitRito().name() : null);
        appendStructuredSignal(signals, "ROUTING_RITO", "ROUTING", 0.85d, false, "Roteamento sugeriu rito", "rito", vector.routingRito() != null ? vector.routingRito().name() : null);
        appendStructuredSignal(signals, "ADVISORY_RITO", "ADVISORY", 0.82d, false, "Camada advisory sugeriu rito", "rito", vector.advisoryRito() != null ? vector.advisoryRito().name() : null);
        appendStructuredSignal(signals, "EXPLICIT_MATERIA", "REQUEST", 0.90d, false, "Matéria explícita informada", "materia", vector.explicitMateria() != null ? vector.explicitMateria().name() : null);
        appendStructuredSignal(signals, "ADVISORY_MATERIA", "ADVISORY", 0.78d, false, "Camada advisory sugeriu matéria", "materia", vector.advisoryMateria() != null ? vector.advisoryMateria().name() : null);
        appendStructuredSignal(signals, "ADVISORY_NATUREZA", "ADVISORY", 0.83d, false, "Camada advisory sugeriu natureza jurídica", "naturezaJuridicaCanonical", vector.advisoryNatureza() != null ? vector.advisoryNatureza().name() : null);
        appendStructuredSignal(signals, "ACTION_NATURE", "ROUTING", 0.73d, false, "Roteamento classificou natureza da ação", "actionNature", vector.actionNature());
        appendStructuredSignal(signals, "ACTION_FAMILY", "ROUTING", 0.73d, false, "Roteamento classificou família de ação", "actionFamily", vector.actionFamily());
        appendConfidenceSignal(signals, "ROUTING_CONFIDENCE", "ROUTING", decimal(source.get("__routingConfidence")), true);
        if (advisory != null) {
            appendConfidenceSignal(signals, "ADVISORY_CONFIDENCE", "ADVISORY", advisory.confidence(), false);
        }
        addTextualSignals(signals, corpus);
        addConflictSignals(signals, vector, advisory);
        return List.copyOf(signals);
    }

    private static void addTextualSignals(List<ProceduralInferenceSignal> signals, String corpus) {
        addTextualSignalIfPresent(signals, corpus, "TUTELA_URGENTE", 0.80d, false, "Texto sugere urgência cautelar", "TUTELA DE URGENCIA", "TUTELA ANTECIPADA", "LIMINAR", "RISCO IMINENTE");
        addTextualSignalIfPresent(signals, corpus, "EXECUCAO", 0.82d, false, "Texto sugere natureza executiva", "EXECUCAO FISCAL", "CUMPRIMENTO DE SENTENCA", "TITULO EXECUTIVO", "COBRANCA EXECUTIVA");
        addTextualSignalIfPresent(signals, corpus, "CONSTITUCIONAL_MANDAMENTAL", 0.84d, true, "Texto sugere natureza mandamental/constitucional", "MANDADO DE SEGURANCA", "HABEAS CORPUS", "HABEAS DATA", "MANDADO DE INJUNCAO");
        addTextualSignalIfPresent(signals, corpus, "VOLUNTARIA", 0.76d, false, "Texto sugere jurisdição voluntária", "INVENTARIO", "ARROLAMENTO", "ALVARA JUDICIAL", "RETIFICACAO DE REGISTRO", "HOMOLOGACAO");
        addTextualSignalIfPresent(signals, corpus, "ESTRUTURAL_COLETIVA", 0.77d, false, "Texto sugere tutela estrutural coletiva", "POLITICA PUBLICA", "DANO COLETIVO", "ACP", "ACAO CIVIL PUBLICA", "TUTELA COLETIVA");
        addTextualSignalIfPresent(signals, corpus, "PENAL_SIGILOSA", 0.73d, false, "Texto sugere cautela de sigilo", "OPERACAO POLICIAL", "INVESTIGACAO", "QUEBRA DE SIGILO", "INQUERITO", "INTERCEPTACAO");
    }

    private static void addConflictSignals(List<ProceduralInferenceSignal> signals, AxisVector vector, ProceduralIntelligenceAdvisoryReport advisory) {
        conflict(signals, "CONFLICT_TIPO_JUSTICA", "tipoJustica", vector.explicitTipo(), vector.routingTipo(), 0.92d, true);
        conflict(signals, "CONFLICT_TIPO_JUSTICA_ADVISORY", "tipoJustica", firstNonNull(vector.routingTipo(), vector.explicitTipo()), vector.advisoryTipo(), 0.88d, false);
        conflict(signals, "CONFLICT_RITO", "rito", vector.explicitRito(), vector.routingRito(), 0.90d, true);
        conflict(signals, "CONFLICT_RITO_ADVISORY", "rito", firstNonNull(vector.routingRito(), vector.explicitRito()), vector.advisoryRito(), 0.86d, false);
        conflict(signals, "CONFLICT_RAMO", "ramoDireito", vector.explicitRamo(), vector.advisoryRamo(), 0.87d, false);
        conflict(signals, "CONFLICT_MATERIA", "materia", vector.explicitMateria(), vector.advisoryMateria(), 0.79d, false);
        if (advisory != null && advisory.fallbackUsed()) {
            signals.add(new ProceduralInferenceSignal.ConflictSignal(
                    "ADVISORY_FALLBACK",
                    "ADVISORY",
                    0.74d,
                    false,
                    "Camada advisory operou com fallback",
                    "fallback",
                    "false",
                    "true"
            ));
        }
    }

    private static List<String> collectConflicts(AxisVector vector, ProceduralIntelligenceAdvisoryReport advisory, List<ProceduralInferenceSignal> signals) {
        LinkedHashSet<String> conflicts = new LinkedHashSet<>();
        signals.stream()
                .filter(ProceduralDecisionQualityEngine::isConflict)
                .map(ProceduralInferenceSignal::summary)
                .forEach(conflicts::add);
        if (advisory != null && advisory.reviewRequired()) {
            conflicts.add("Camada advisory já sinalizou revisão humana");
        }
        return List.copyOf(conflicts);
    }

    private static List<String> collectConsensus(AxisVector vector, ProceduralIntelligenceAdvisoryReport advisory) {
        LinkedHashSet<String> consensus = new LinkedHashSet<>();
        if (vector.explicitTipo() != null && vector.explicitTipo() == vector.routingTipo()) {
            consensus.add("Tipo de justiça convergente entre request e roteamento");
        }
        if (vector.routingTipo() != null && vector.routingTipo() == vector.advisoryTipo()) {
            consensus.add("Tipo de justiça convergente entre roteamento e advisory");
        }
        if (vector.explicitRito() != null && vector.explicitRito() == vector.routingRito()) {
            consensus.add("Rito convergente entre request e roteamento");
        }
        if (vector.routingRito() != null && vector.routingRito() == vector.advisoryRito()) {
            consensus.add("Rito convergente entre roteamento e advisory");
        }
        if (vector.explicitRamo() != null && vector.explicitRamo() == vector.advisoryRamo()) {
            consensus.add("Ramo jurídico convergente entre request e advisory");
        }
        if (vector.explicitMateria() != null && vector.explicitMateria() == vector.advisoryMateria()) {
            consensus.add("Matéria convergente entre request e advisory");
        }
        if (advisory != null && advisory.naturezaPrincipal() != null && vector.actionNature() != null && normalize(vector.actionNature()).contains(normalize(advisory.naturezaPrincipal().name()))) {
            consensus.add("Natureza jurídica advisory converge com classificação do roteamento");
        }
        return List.copyOf(consensus);
    }

    private static double computeConvergence(AxisVector vector, List<String> conflicts, ProceduralIntelligenceAdvisoryReport advisory) {
        double score = 0.22d;
        if (vector.explicitTipo() != null && vector.explicitTipo() == vector.routingTipo()) {
            score += 0.15d;
        }
        if (vector.routingTipo() != null && vector.routingTipo() == vector.advisoryTipo()) {
            score += 0.11d;
        }
        if (vector.explicitRito() != null && vector.explicitRito() == vector.routingRito()) {
            score += 0.16d;
        }
        if (vector.routingRito() != null && vector.routingRito() == vector.advisoryRito()) {
            score += 0.12d;
        }
        if (vector.explicitRamo() != null && vector.explicitRamo() == vector.advisoryRamo()) {
            score += 0.09d;
        }
        if (vector.explicitMateria() != null && vector.explicitMateria() == vector.advisoryMateria()) {
            score += 0.08d;
        }
        if (advisory != null && !advisory.fallbackUsed()) {
            score += 0.06d;
        }
        score -= Math.min(0.32d, conflicts.size() * 0.08d);
        return score;
    }

    private static double computeEvidenceScore(List<ProceduralInferenceSignal> signals, List<String> consensus, ProceduralIntelligenceAdvisoryReport advisory) {
        if (signals.isEmpty()) {
            return 0.18d;
        }
        double weightSum = signals.stream().mapToDouble(ProceduralInferenceSignal::weight).sum();
        long criticalSignals = signals.stream().filter(ProceduralInferenceSignal::critical).count();
        long origins = signals.stream().map(ProceduralInferenceSignal::origin).filter(Objects::nonNull).distinct().count();
        double normalizedWeight = clamp01(weightSum / 12d);
        double criticalBoost = Math.min(0.16d, criticalSignals * 0.022d);
        double originBoost = Math.min(0.18d, origins * 0.045d);
        double consensusBoost = Math.min(0.14d, consensus.size() * 0.04d);
        double advisoryBoost = advisory == null ? 0d : Math.min(0.15d, advisory.confidence() * 0.15d);
        return normalizedWeight * 0.47d + criticalBoost + originBoost + consensusBoost + advisoryBoost;
    }

    private static double computeReviewPressure(List<String> conflicts,
                                                List<String> riskSignals,
                                                ProceduralIntelligenceAdvisoryReport advisory,
                                                Map<String, Object> source) {
        double pressure = 0.12d;
        pressure += Math.min(0.38d, conflicts.size() * 0.09d);
        pressure += Math.min(0.20d, riskSignals.size() * 0.025d);
        if (advisory != null) {
            if (advisory.reviewRequired()) {
                pressure += 0.20d;
            }
            if (advisory.fallbackUsed()) {
                pressure += 0.11d;
            }
            if (advisory.confidence() < 0.64d) {
                pressure += 0.10d;
            }
        }
        String riskLevel = text(source.get("__riskLevel"));
        if (riskLevel != null) {
            String normalized = normalize(riskLevel);
            if (normalized.contains("ALTO")) {
                pressure += 0.16d;
            } else if (normalized.contains("MEDIO")) {
                pressure += 0.08d;
            }
        }
        return pressure;
    }

    private static String resolveOperatingMode(boolean safeAutomationEligible,
                                               double determinismScore,
                                               double reviewPressureScore,
                                               ProceduralIntelligenceAdvisoryReport advisory) {
        if (safeAutomationEligible) {
            return "AUTOMATE_SAFE";
        }
        if (reviewPressureScore >= 0.68d || (advisory != null && advisory.reviewRequired())) {
            return "HUMAN_REVIEW";
        }
        if (determinismScore >= 0.58d) {
            return "ASSISTED_DECISION";
        }
        return "ADVISORY_ONLY";
    }

    private static void appendStructuredSignal(List<ProceduralInferenceSignal> signals,
                                               String code,
                                               String origin,
                                               double weight,
                                               boolean critical,
                                               String summary,
                                               String field,
                                               String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        signals.add(new ProceduralInferenceSignal.StructuredSignal(code, origin, weight, critical, summary, field, value));
    }

    private static void appendConfidenceSignal(List<ProceduralInferenceSignal> signals,
                                               String code,
                                               String origin,
                                               double confidence,
                                               boolean critical) {
        if (confidence <= 0d) {
            return;
        }
        signals.add(new ProceduralInferenceSignal.ConfidenceSignal(
                code,
                origin,
                clamp01(confidence),
                critical,
                "Sinal de confiança numérica disponível",
                clamp01(confidence),
                confidenceBand(confidence)
        ));
    }

    private static void addTextualSignalIfPresent(List<ProceduralInferenceSignal> signals,
                                                  String corpus,
                                                  String code,
                                                  double weight,
                                                  boolean critical,
                                                  String summary,
                                                  String... patterns) {
        for (String pattern : patterns) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            String normalizedPattern = normalize(pattern);
            if (corpus.contains(normalizedPattern)) {
                signals.add(new ProceduralInferenceSignal.TextualSignal(
                        code,
                        "TEXT",
                        weight,
                        critical,
                        summary,
                        pattern,
                        normalizedPattern
                ));
                return;
            }
        }
    }

    private static void conflict(List<ProceduralInferenceSignal> signals,
                                 String code,
                                 String axis,
                                 Object left,
                                 Object right,
                                 double weight,
                                 boolean critical) {
        if (left == null || right == null) {
            return;
        }
        if (Objects.equals(left, right)) {
            return;
        }
        signals.add(new ProceduralInferenceSignal.ConflictSignal(
                code,
                "QUALITY",
                weight,
                critical,
                "Conflito detectado no eixo " + axis,
                axis,
                stringify(left),
                stringify(right)
        ));
    }

    private static boolean isConflict(ProceduralInferenceSignal signal) {
        return signal instanceof ProceduralInferenceSignal.ConflictSignal;
    }

    private static String confidenceBand(double value) {
        if (value >= 0.90d) {
            return "VERY_HIGH";
        }
        if (value >= 0.75d) {
            return "HIGH";
        }
        if (value >= 0.60d) {
            return "MEDIUM";
        }
        if (value > 0d) {
            return "LOW";
        }
        return "UNKNOWN";
    }

    private static String buildCorpus(Map<String, Object> source) {
        StringBuilder sb = new StringBuilder();
        appendValue(sb, source.get("assunto"));
        appendValue(sb, source.get("objetoProcessual"));
        appendValue(sb, source.get("pedidoPrincipal"));
        appendValue(sb, source.get("pedidosConsolidados"));
        appendValue(sb, source.get("classeProcessual"));
        appendValue(sb, source.get("classeTpuCodigo"));
        appendValue(sb, source.get("materialProbatorioResumo"));
        appendValue(sb, source.get("__routingActionNature"));
        appendValue(sb, source.get("__routingActionFamily"));
        appendValue(sb, source.get("canonical:classeTpuCodigo"));
        appendValue(sb, source.get("canonical:classeTpuNome"));
        return normalize(sb.toString());
    }

    private static void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> appendValue(sb, item));
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(value);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static double decimal(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0d;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0d;
        }
    }

    private static double clamp01(double value) {
        if (value < 0d) {
            return 0d;
        }
        return Math.min(1d, value);
    }

    private static String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private record AxisVector(
            TipoJustica explicitTipo,
            TipoJustica routingTipo,
            TipoJustica advisoryTipo,
            RamoDireito explicitRamo,
            RamoDireito advisoryRamo,
            RitoProcessual explicitRito,
            RitoProcessual routingRito,
            RitoProcessual advisoryRito,
            MateriaJurisdicao explicitMateria,
            MateriaJurisdicao advisoryMateria,
            NaturezaJuridicaCanonical advisoryNatureza,
            String actionNature,
            String actionFamily
    ) {
        Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("explicitTipo", explicitTipo != null ? explicitTipo.name() : null);
            out.put("routingTipo", routingTipo != null ? routingTipo.name() : null);
            out.put("advisoryTipo", advisoryTipo != null ? advisoryTipo.name() : null);
            out.put("explicitRamo", explicitRamo != null ? explicitRamo.name() : null);
            out.put("advisoryRamo", advisoryRamo != null ? advisoryRamo.name() : null);
            out.put("explicitRito", explicitRito != null ? explicitRito.name() : null);
            out.put("routingRito", routingRito != null ? routingRito.name() : null);
            out.put("advisoryRito", advisoryRito != null ? advisoryRito.name() : null);
            out.put("explicitMateria", explicitMateria != null ? explicitMateria.name() : null);
            out.put("advisoryMateria", advisoryMateria != null ? advisoryMateria.name() : null);
            out.put("advisoryNatureza", advisoryNatureza != null ? advisoryNatureza.name() : null);
            out.put("actionNature", actionNature);
            out.put("actionFamily", actionFamily);
            out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
            return Collections.unmodifiableMap(out);
        }
    }
}
