package com.tcc.pjb.backend.configs.datasource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class PjbInstitutionalRoutingAugmentedRequest extends HttpServletRequestWrapper {

    private final Map<String, List<String>> mergedHeaders;
    private final Map<String, String> lookup;

    public PjbInstitutionalRoutingAugmentedRequest(HttpServletRequest request, Map<String, String> additionalHeaders) {
        super(Objects.requireNonNull(request, "request"));
        Map<String, List<String>> incoming = copyIncomingHeaders(request);
        Map<String, String> normalizedLookup = new LinkedHashMap<>();
        incoming.keySet().forEach(name -> normalizedLookup.put(normalize(name), name));
        if (additionalHeaders != null) {
            additionalHeaders.forEach((name, value) -> {
                if (name == null || name.isBlank() || value == null || value.isBlank()) {
                    return;
                }
                String normalized = normalize(name);
                String actualName = normalizedLookup.getOrDefault(normalized, name.trim());
                incoming.computeIfAbsent(actualName, ignored -> new ArrayList<>()).add(value.trim());
                normalizedLookup.put(normalized, actualName);
            });
        }
        LinkedHashMap<String, List<String>> immutable = new LinkedHashMap<>();
        incoming.forEach((name, values) -> immutable.put(name, List.copyOf(values)));
        this.mergedHeaders = Collections.unmodifiableMap(immutable);
        this.lookup = Collections.unmodifiableMap(normalizedLookup);
    }

    @Override
    public String getHeader(String name) {
        List<String> values = resolveValues(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = resolveValues(name);
        return Collections.enumeration(values == null ? List.of() : values);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(new LinkedHashSet<>(mergedHeaders.keySet()));
    }

    private List<String> resolveValues(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String actual = lookup.get(normalize(name));
        return actual == null ? null : mergedHeaders.get(actual);
    }

    private Map<String, List<String>> copyIncomingHeaders(HttpServletRequest request) {
        LinkedHashMap<String, List<String>> out = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            List<String> values = Collections.list(request.getHeaders(name));
            out.put(name, new ArrayList<>(values));
        }
        return out;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
