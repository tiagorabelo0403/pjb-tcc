package com.tcc.pjb.backend.service.painel.shared;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PainelNativeCollectionCompositionService {

    public <T> List<T> composeList(String panelCode,
                                   String collectionName,
                                   List<T> source,
                                   Map<String, Object> operationalSignals,
                                   Map<String, Object> nativeComposition) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        Map<String, Object> safeOperationalSignals = map(operationalSignals);
        Map<String, Object> safeNativeComposition = map(nativeComposition);
        List<T> out = new ArrayList<>(source);
        String orderingMode = string(safeNativeComposition.getOrDefault("listOrderingMode", safeOperationalSignals.getOrDefault("listOrderingMode", "BALANCED")));
        out.sort((left, right) -> compareValues(left, right, panelCode, collectionName, orderingMode));
        return List.copyOf(out);
    }

    public Map<String, Object> buildCollectionComposition(String panelCode,
                                                          Map<String, Object> operationalSignals,
                                                          Map<String, Object> nativeComposition,
                                                          Map<String, ? extends List<?>> collections) {
        Map<String, Object> safeOperationalSignals = map(operationalSignals);
        Map<String, Object> safeNativeComposition = map(nativeComposition);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("panelCode", string(panelCode));
        out.put("priorityTag", string(safeOperationalSignals.getOrDefault("priorityTag", "NORMAL")));
        out.put("attentionScore", intValue(safeOperationalSignals.get("attentionScore")));
        out.put("listOrderingMode", string(safeNativeComposition.getOrDefault("listOrderingMode", safeOperationalSignals.getOrDefault("listOrderingMode", "BALANCED"))));
        out.put("compositionMode", string(safeNativeComposition.getOrDefault("compositionMode", safeOperationalSignals.getOrDefault("compositionMode", "BALANCED_FLOW"))));
        out.put("highlightedSection", string(safeNativeComposition.getOrDefault("highlightedSection", "PAINEL")));
        LinkedHashMap<String, Object> directives = new LinkedHashMap<>();
        if (collections != null) {
            collections.forEach((name, values) -> {
                String safeName = string(name);
                LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                item.put("size", values == null ? 0 : values.size());
                item.put("highlighted", shouldHighlight(safeName, safeNativeComposition));
                item.put("orderingMode", out.get("listOrderingMode"));
                item.put("collapseSuggested", bool(safeNativeComposition.get("collapseSecondaryBlocks")) && !shouldHighlight(safeName, safeNativeComposition));
                item.put("emptyState", values == null || values.isEmpty() ? "EMPTY" : "READY");
                directives.put(safeName, Map.copyOf(item));
            });
        }
        out.put("collections", Map.copyOf(directives));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> decorateBlock(String panelCode,
                                             String blockName,
                                             Map<String, Object> block,
                                             Map<String, Object> operationalSignals,
                                             Map<String, Object> nativeComposition) {
        LinkedHashMap<String, Object> out = block == null ? new LinkedHashMap<>() : new LinkedHashMap<>(block);
        LinkedHashMap<String, List<?>> extracted = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : out.entrySet()) {
            if (entry.getValue() instanceof List<?> listValue) {
                List<?> composed = composeList(panelCode, blockName + ":" + entry.getKey(), listValue, operationalSignals, nativeComposition);
                out.put(entry.getKey(), composed);
                extracted.put(string(entry.getKey()), composed);
            }
        }
        out.put("collectionComposition", buildCollectionComposition(panelCode, operationalSignals, nativeComposition, extracted));
        return Collections.unmodifiableMap(out);
    }

    private boolean shouldHighlight(String name, Map<String, Object> nativeComposition) {
        String normalizedName = normalize(name);
        String normalizedHighlighted = normalize(string(map(nativeComposition).getOrDefault("highlightedSection", "PAINEL")));
        if (normalizedHighlighted.isBlank()) {
            return false;
        }
        return normalizedName.contains(normalizedHighlighted) || normalizedHighlighted.contains(normalizedName);
    }

    private int compareValues(Object left, Object right, String panelCode, String collectionName, String orderingMode) {
        int weightCompare = Integer.compare(weight(right, panelCode, collectionName, orderingMode), weight(left, panelCode, collectionName, orderingMode));
        if (weightCompare != 0) {
            return weightCompare;
        }
        Instant leftInstant = instantValue(left);
        Instant rightInstant = instantValue(right);
        if (leftInstant != null && rightInstant != null) {
            return leftInstant.compareTo(rightInstant);
        }
        return normalize(String.valueOf(left)).compareTo(normalize(String.valueOf(right)));
    }

    private int weight(Object value, String panelCode, String collectionName, String orderingMode) {
        int score = 0;
        String normalized = normalize(String.valueOf(value));
        if (normalized.contains("URGENT") || normalized.contains("URGENTE") || normalized.contains("CRITIC") || normalized.contains("PRIORIT")) {
            score += 100;
        }
        if (normalized.contains("PRAZO") || normalized.contains("24H") || normalized.contains("48H")) {
            score += 70;
        }
        if (normalized.contains("HABEAS") || normalized.contains("PRISAO") || normalized.contains("PLANTAO")) {
            score += 80;
        }
        if (normalized.contains("SESSAO") || normalized.contains("PAUTA") || normalized.contains("ACORDAO") || normalized.contains("VISTA")) {
            score += 65;
        }
        if (normalized.contains("MANDADO") || normalized.contains("PENHORA") || normalized.contains("DILIGENCIA")) {
            score += 60;
        }
        if (normalize(collectionName).contains("PRIORIDADE") || normalize(collectionName).contains("URGENTE")) {
            score += 25;
        }
        if (Objects.equals(orderingMode, "URGENT_FIRST")) {
            score += 15;
        } else if (Objects.equals(orderingMode, "PRIORITY_FIRST")) {
            score += 10;
        }
        if (normalize(panelCode).contains("OFICIAL") && normalized.contains("MANDADO")) {
            score += 20;
        }
        if (normalize(panelCode).contains("DELEGADO") && (normalized.contains("INQUERITO") || normalized.contains("TRIAGEM"))) {
            score += 20;
        }
        return score;
    }

    private Instant instantValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        String raw = String.valueOf(value);
        if (raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> casted) {
            return (Map<String, Object>) casted;
        }
        return Map.of();
    }

    private boolean bool(Object value) {
        return value instanceof Boolean casted ? casted : Boolean.parseBoolean(string(value));
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(string(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
