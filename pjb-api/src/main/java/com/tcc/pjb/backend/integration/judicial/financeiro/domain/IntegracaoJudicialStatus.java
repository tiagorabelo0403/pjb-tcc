package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

import java.util.Locale;
import java.util.Map;

public enum IntegracaoJudicialStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    FAILED;

    private static final Map<String, IntegracaoJudicialStatus> ALIASES = Map.ofEntries(
            Map.entry("PENDENTE", PENDING),
            Map.entry("PENDING", PENDING),
            Map.entry("CONFIRMED", CONFIRMED),
            Map.entry("CONFIRMADO", CONFIRMED),
            Map.entry("REJECTED", REJECTED),
            Map.entry("REJEITADO", REJECTED),
            Map.entry("CANCELLED", CANCELLED),
            Map.entry("CANCELED", CANCELLED),
            Map.entry("CANCELADO", CANCELLED),
            Map.entry("FAILED", FAILED),
            Map.entry("FALHA", FAILED)
    );

    public static IntegracaoJudicialStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ALIASES.get(value.trim().toUpperCase(Locale.ROOT));
    }
}
