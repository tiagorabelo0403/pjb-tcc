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
    ESTADO("ESTADO", "Estado"),
    ACUSACAO("ACUSACAO", "Acusação"),
    ACUSADO("ACUSADO", "Acusado"),
    RECLAMANTE("RECLAMANTE", "Reclamante"),
    RECLAMADA("RECLAMADA", "Reclamada"),
    EMPREGADOR("EMPREGADOR", "Empregador"),
    EMPREGADO_ESTAVEL("EMPREGADO_ESTAVEL", "Empregado estável"),
    SUBSTITUTO_PROCESSUAL("SUBSTITUTO_PROCESSUAL", "Substituto processual"),
    SUSCITANTE("SUSCITANTE", "Suscitante"),
    SUSCITADO("SUSCITADO", "Suscitado"),
    IMPETRANTE("IMPETRANTE", "Impetrante"),
    IMPETRADO("IMPETRADO", "Impetrado"),
    REPRESENTANTE("REPRESENTANTE", "Representante"),
    REPRESENTADO("REPRESENTADO", "Representado"),
    IMPUGNANTE("IMPUGNANTE", "Impugnante"),
    IMPUGNADO("IMPUGNADO", "Impugnado"),
    INVESTIGADO("INVESTIGADO", "Investigado"),
    REQUERENTE("REQUERENTE", "Requerente"),
    REQUERIDO("REQUERIDO", "Requerido"),
    AUTOR_POPULAR("AUTOR_POPULAR", "Autor popular"),
    MENOR("MENOR", "Menor"),
    SEGURADO("SEGURADO", "Segurado"),
    CANDIDATO("CANDIDATO", "Candidato"),
    SERVIDOR("SERVIDOR", "Servidor"),
    PRESTADOR_CONTAS("PRESTADOR_CONTAS", "Prestador de contas"),
    DEVEDOR("DEVEDOR", "Devedor");

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
