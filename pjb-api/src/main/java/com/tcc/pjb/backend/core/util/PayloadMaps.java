package com.tcc.pjb.backend.core.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PayloadMaps {

    private PayloadMaps() {
    }

    public static Map<String, Object> ofEntries(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Map.of();
        }
        if ((keyValues.length & 1) != 0) {
            throw new IllegalArgumentException("Chaves e valores devem ser informados em pares");
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            Object rawKey = keyValues[i];
            if (!(rawKey instanceof String key) || key.isBlank()) {
                throw new IllegalArgumentException("Chave inválida na posição " + i);
            }
            Object value = keyValues[i + 1];
            if (value != null) {
                values.put(key, value);
            }
        }
        return values.isEmpty() ? Map.of() : Map.copyOf(values);
    }

    public static Map<String, Object> copyWithoutNulls(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                values.put(key, value);
            }
        });
        return values.isEmpty() ? Map.of() : Map.copyOf(values);
    }


    public static Map<String, Object> deepCopyWithoutNulls(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key == null || key.isBlank()) {
                return;
            }
            Object sanitized = sanitize(value);
            if (sanitized != null) {
                values.put(key, sanitized);
            }
        });
        return values.isEmpty() ? Map.of() : Map.copyOf(values);
    }

    public static <T> List<T> copyListWithoutNulls(Iterable<? extends T> source) {
        if (source == null) {
            return List.of();
        }
        ArrayList<T> values = new ArrayList<>();
        for (T value : source) {
            if (value != null) {
                values.add(value);
            }
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }



    public static <T> List<T> copyListDistinct(Iterable<? extends T> source) {
        if (source == null) {
            return List.of();
        }
        java.util.LinkedHashSet<T> values = new java.util.LinkedHashSet<>();
        for (T value : source) {
            if (value != null) {
                values.add(value);
            }
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    public static List<String> copyDistinctStrings(Iterable<String> source) {
        if (source == null) {
            return List.of();
        }
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        for (String value : source) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    public static List<String> copyTrimmedStrings(Iterable<String> source) {
        if (source == null) {
            return List.of();
        }
        ArrayList<String> values = new ArrayList<>();
        for (String value : source) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    private static Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            String trimmed = s.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
            map.forEach((nestedKey, nestedValue) -> {
                if (nestedKey instanceof String key && !key.isBlank()) {
                    Object sanitized = sanitize(nestedValue);
                    if (sanitized != null) {
                        nested.put(key, sanitized);
                    }
                }
            });
            return nested.isEmpty() ? null : Map.copyOf(nested);
        }
        if (value instanceof Iterable<?> iterable) {
            ArrayList<Object> nested = new ArrayList<>();
            for (Object item : iterable) {
                Object sanitized = sanitize(item);
                if (sanitized != null) {
                    nested.add(sanitized);
                }
            }
            return nested.isEmpty() ? null : List.copyOf(nested);
        }
        return value;
    }

    public static Map<String, Object> merge(Map<String, ?> primary, Map<String, ?> secondary) {
        if ((primary == null || primary.isEmpty()) && (secondary == null || secondary.isEmpty())) {
            return Map.of();
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        if (primary != null) {
            primary.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    values.put(key, value);
                }
            });
        }
        if (secondary != null) {
            secondary.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    values.put(key, value);
                }
            });
        }
        return values.isEmpty() ? Map.of() : Map.copyOf(values);
    }
}
