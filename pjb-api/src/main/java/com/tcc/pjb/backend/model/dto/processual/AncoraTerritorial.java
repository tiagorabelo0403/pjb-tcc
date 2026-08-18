package com.tcc.pjb.backend.model.dto.processual;

import java.util.Locale;
import java.util.regex.Pattern;

public record AncoraTerritorial(
        String municipioIbge,
        String municipio,
        String uf
) {
    private static final Pattern IBGE_PATTERN = Pattern.compile("^[0-9]{7}$");

    public AncoraTerritorial {
        municipioIbge = normalizeIbge(municipioIbge);
        municipio = trimToNull(municipio);
        uf = normalizeUf(uf);
    }

    public boolean resolvivel() {
        return municipioIbge != null;
    }

    private static String normalizeIbge(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null || !IBGE_PATTERN.matcher(trimmed).matches()) {
            return null;
        }
        return trimmed;
    }

    private static String normalizeUf(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
