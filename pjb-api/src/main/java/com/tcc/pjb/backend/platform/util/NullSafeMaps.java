package com.tcc.pjb.backend.platform.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NullSafeMaps {

    private NullSafeMaps() {
    }

    public static Map<String, Object> empty() {
        return Collections.emptyMap();
    }

    public static Builder linked() {
        return new Builder();
    }

    public static final class Builder {
        private final LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        public Builder put(String key, Object value) {
            if (key == null) return this;
            String k = key.trim();
            if (k.isEmpty()) return this;
            if (value == null) return this;
            map.put(k, value);
            return this;
        }

        public Builder putAll(Map<String, ?> in) {
            if (in == null || in.isEmpty()) return this;
            for (Map.Entry<String, ?> e : in.entrySet()) {
                put(e.getKey(), e.getValue());
            }
            return this;
        }

        public Map<String, Object> build() {
            if (map.isEmpty()) return Collections.emptyMap();
            return Collections.unmodifiableMap(new LinkedHashMap<>(map));
        }
    }
}
