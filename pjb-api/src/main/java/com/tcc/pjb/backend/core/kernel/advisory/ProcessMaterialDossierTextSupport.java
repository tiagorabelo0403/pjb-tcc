package com.tcc.pjb.backend.core.kernel.advisory;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ProcessMaterialDossierTextSupport {

    List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String cleaned = sanitizeBullet(value);
            if (cleaned != null && !cleaned.isBlank()) {
                out.add(cleaned);
            }
        }
        return List.copyOf(out);
    }

    List<String> normalizeListObject(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            LinkedHashSet<String> out = new LinkedHashSet<>();
            for (Object item : list) {
                String cleaned = sanitizeBullet(asString(item));
                if (cleaned != null && !cleaned.isBlank()) {
                    out.add(cleaned);
                }
            }
            return List.copyOf(out);
        }
        return splitStructured(asString(value));
    }

    List<String> splitStructured(String value) {
        if (blank(value)) {
            return List.of();
        }
        String[] parts = value.replace('\r', '\n').split("\\R|;|\\|");
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String part : parts) {
            String cleaned = sanitizeBullet(part);
            if (cleaned != null && !cleaned.isBlank()) {
                out.add(cleaned);
            }
        }
        return List.copyOf(out);
    }

    String joinText(String... values) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String cleaned = trimToNull(value);
                if (cleaned != null) {
                    out.add(cleaned);
                }
            }
        }
        return out.isEmpty() ? null : String.join(System.lineSeparator(), out);
    }

    String sanitizeBullet(String value) {
        String cleaned = trimToNull(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.replaceFirst("^[\\-•*\\d.)\\s]+", "").trim();
    }

    boolean containsAny(String token, String... needles) {
        if (blank(token) || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (!blank(needle) && token.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    int countKeywords(String token, Set<String> keywords) {
        if (blank(token) || keywords == null || keywords.isEmpty()) {
            return 0;
        }
        int hits = 0;
        for (String keyword : keywords) {
            if (!blank(keyword) && token.contains(keyword)) {
                hits++;
            }
        }
        return hits;
    }

    String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
        return normalized.replaceAll("[^A-Z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String cleaned = trimToNull(value);
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    String firstBullet(String value) {
        return firstItem(splitStructured(value));
    }

    String firstItem(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        for (String value : values) {
            String cleaned = trimToNull(value);
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    boolean truthy(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.length() <= max) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, max));
    }

    @SafeVarargs
    static List<String> mergeOrderedLists(List<String>... sources) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (sources != null) {
            for (List<String> source : sources) {
                if (source != null) {
                    merged.addAll(source);
                }
            }
        }
        return List.copyOf(merged);
    }

    @SafeVarargs
    static LinkedHashSet<String> mergeOrderedSet(List<String>... sources) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (sources != null) {
            for (List<String> source : sources) {
                if (source != null) {
                    merged.addAll(source);
                }
            }
        }
        return merged;
    }

    List<String> limitNormalized(List<String> values, int limit) {
        if (values == null || values.isEmpty() || limit <= 0) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String normalized = value.trim();
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
            if (out.size() >= limit) {
                break;
            }
        }
        return List.copyOf(out);
    }
}
