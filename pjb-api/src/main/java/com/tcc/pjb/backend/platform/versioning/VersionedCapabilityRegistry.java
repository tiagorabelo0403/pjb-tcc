package com.tcc.pjb.backend.platform.versioning;

import java.util.Collections;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

public final class VersionedCapabilityRegistry<T> {

    public static final String DEFAULT_CAPABILITY = "*";

    private final Map<ApiVersion, Map<String, T>> table = new EnumMap<>(ApiVersion.class);

    public VersionedCapabilityRegistry() {
        for (ApiVersion v : ApiVersion.values()) {
            table.put(v, new ConcurrentHashMap<>());
        }
    }

    public VersionedCapabilityRegistry<T> register(ApiVersion version, String capability, T impl) {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(impl, "impl");
        String cap = normalizeCapability(capability);
        table.get(version).put(cap, impl);
        return this;
    }

    public Optional<T> resolve(ApiVersion version, String capability) {
        if (version == null) return Optional.empty();
        String cap = normalizeCapability(capability);
        Map<String, T> vmap = table.get(version);
        if (vmap == null) return Optional.empty();
        T impl = vmap.get(cap);
        if (impl != null) return Optional.of(impl);
        return Optional.ofNullable(vmap.get(DEFAULT_CAPABILITY));
    }

    public Optional<T> resolveWithFallback(ApiVersion requested, String capability) {
        ApiVersion v = requested != null ? requested : ApiVersion.latest();
        Optional<T> hit = resolve(v, capability);
        if (hit.isPresent()) return hit;

        Optional<ApiVersion> prev = v.previous();
        while (prev.isPresent()) {
            v = prev.get();
            hit = resolve(v, capability);
            if (hit.isPresent()) return hit;
            prev = v.previous();
        }
        return Optional.empty();
    }

    public Map<ApiVersion, Map<String, T>> snapshot() {
        EnumMap<ApiVersion, Map<String, T>> copy = new EnumMap<>(ApiVersion.class);
        for (var e : table.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableMap(new HashMap<>(e.getValue())));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static String normalizeCapability(String capability) {
        String c = (capability == null || capability.isBlank()) ? DEFAULT_CAPABILITY : capability;
        return c.trim().toUpperCase(Locale.ROOT);
    }
}
