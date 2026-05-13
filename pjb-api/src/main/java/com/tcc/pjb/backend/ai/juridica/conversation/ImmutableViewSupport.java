package com.tcc.pjb.backend.ai.juridica.conversation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ImmutableViewSupport {

    private ImmutableViewSupport() {
    }

    public static <K, V> Map<K, V> map(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
