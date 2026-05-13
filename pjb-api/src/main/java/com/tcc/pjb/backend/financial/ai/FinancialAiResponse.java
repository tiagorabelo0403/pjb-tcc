package com.tcc.pjb.backend.financial.ai;

import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record FinancialAiResponse(
        String requestId,
        String correlationId,
        String operation,
        String origin,
        ApiVersion version,
        FinancialAiStatus status,
        double confidence,
        String message,
        Instant timestamp,
        Map<String, Object> outputs,
        List<String> warnings,
        Set<String> capabilities
) {
    public FinancialAiResponse {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(status, "status");
        timestamp = timestamp != null ? timestamp : Instant.now();
        outputs = outputs == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(outputs));
        warnings = warnings == null
                ? List.of()
                : List.copyOf(warnings);
        capabilities = capabilities == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(capabilities));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> outputMap(String key) {
        if (key == null || key.isBlank()) {
            return Map.of();
        }
        Object value = outputs.get(key);
        if (value instanceof Map<?, ?> raw) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            raw.forEach((k, v) -> {
                if (k != null) {
                    mapped.put(String.valueOf(k), v);
                }
            });
            return Collections.unmodifiableMap(mapped);
        }
        return Map.of();
    }

    public Object output(String key) {
        return key == null ? null : outputs.get(key);
    }

    public BigDecimal outputDecimal(String key) {
        Object value = output(key);
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text) {
            try {
                return new BigDecimal(text.replace(',', '.').trim());
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    public String messageOr(String fallback) {
        return message != null && !message.isBlank() ? message : fallback;
    }
}
