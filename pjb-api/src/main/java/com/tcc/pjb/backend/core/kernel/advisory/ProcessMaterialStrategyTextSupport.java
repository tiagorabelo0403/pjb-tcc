package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class ProcessMaterialStrategyTextSupport {

    List<String> safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String cleaned = trimToNull(value);
            if (cleaned != null) {
                out.add(cleaned);
            }
        }
        return List.copyOf(out);
    }

    String joinStructured(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String cleaned = trimToNull(value);
            if (cleaned != null) {
                out.add(cleaned);
            }
        }
        return out.isEmpty() ? null : String.join(" | ", out);
    }

    String stringFromCtx(LaianePeticaoAssistRequest request, String... keys) {
        if (request == null || request.getCtx() == null || request.getCtx().isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object value = request.getCtx().get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object item : list) {
                    String cleaned = trimToNull(item == null ? null : String.valueOf(item));
                    if (cleaned != null) {
                        out.add(cleaned);
                    }
                }
                if (!out.isEmpty()) {
                    return String.join(" | ", out);
                }
                continue;
            }
            String asString = trimToNull(String.valueOf(value));
            if (asString != null) {
                return asString;
            }
        }
        return null;
    }

    String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    boolean containsAny(String raw, String... needles) {
        String normalized = normalize(raw);
        if (normalized == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            String token = normalize(needle);
            if (token != null && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String compact = normalized.replaceAll("[^A-Za-z0-9]+", " ").trim();
        return compact.isEmpty() ? null : compact.toUpperCase(Locale.ROOT);
    }

    String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    boolean blank(String value) {
        return trimToNull(value) == null;
    }

    String compact(String value, int max) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String cleaned = trimmed.replaceAll("\\s+", " ");
        if (cleaned.length() <= max) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, max));
    }
}
