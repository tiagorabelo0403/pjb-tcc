package com.tcc.pjb.backend.model.entity.enums;

import java.util.Locale;
import java.util.Optional;

public enum TipoParte {
    AUTOR("PARTE_AUTORA", "Parte autora"),
    REU("PARTE_RE", "Parte ré"),
    TERCEIRO_INTERESSADO("TERCEIRO_INTERESSADO", "Terceiro interessado"),
    MINISTERIO_PUBLICO("MINISTERIO_PUBLICO", "Ministério Público"),
    DESEMBARGADOR("DESEMBARGADOR", "Desembargador"),
    MINISTRO("MINISTRO", "Ministro"),
    DELEGADO("DELEGADO", "Delegado"),
    MUNICIPIO("MUNICIPIO", "Município"),
    ESTADO("ESTADO", "Estado");

    private final String codigo;
    private final String rotulo;

    TipoParte(String codigo, String rotulo) {
        this.codigo = codigo;
        this.rotulo = rotulo;
    }

    public String codigo() {
        return codigo;
    }

    public String rotulo() {
        return rotulo;
    }

    public boolean isPartePrincipal() {
        return this == AUTOR || this == REU;
    }

    public static Optional<TipoParte> tryParse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String token = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        for (TipoParte value : values()) {
            if (value.name().equals(token) || value.codigo.equals(token)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
