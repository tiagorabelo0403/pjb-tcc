package com.tcc.pjb.backend.core.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SafeMaps {

    private SafeMaps() {
    }

    
    public static Map<String, Object> of(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Collections.emptyMap();
        }
        if ((keyValues.length & 1) != 0) {
            throw new IllegalArgumentException("SafeMaps.of requires an even number of arguments (key/value pairs)");
        }

        LinkedHashMap<String, Object> out = new LinkedHashMap<>(Math.max(8, keyValues.length / 2));
        for (int i = 0; i < keyValues.length; i += 2) {
            Object rawKey = keyValues[i];
            String key = (rawKey == null) ? null : (rawKey instanceof String s ? s : String.valueOf(rawKey));
            Object value = keyValues[i + 1];

            
            if (out.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate key in SafeMaps.of: " + Objects.toString(key));
            }
            out.put(key, value);
        }
        return Collections.unmodifiableMap(out);
    }

    
    public static <K, V> Map<K, V> ofNullable(Map<K, V> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(input));
    }

    
    public static Map<String, Object> ofNullable(Object... keyValues) {
        return (keyValues == null || keyValues.length == 0) ? Collections.emptyMap() : of(keyValues);
    }

    public static Map<String, Object> empty() {
        return Collections.emptyMap();
    }
}
