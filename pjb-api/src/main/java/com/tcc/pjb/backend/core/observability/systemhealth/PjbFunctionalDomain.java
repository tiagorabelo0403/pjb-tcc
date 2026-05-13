package com.tcc.pjb.backend.core.observability.systemhealth;

import java.util.List;

public enum PjbFunctionalDomain {
    PETICIONAMENTO(List.of("/api/v1/peticionamento", "/api/v1/laiane", "/api/v1/processual/routing/diagnostico")),
    CONSULTA(List.of("/api/v1/processos", "/api/v1/processo", "/api/v1/timeline", "/api/v1/workspace", "/api/v1/jurisprudencia", "/api/v1/jurisdicao")),
    OPERACAO_INTERNA(List.of("/api/v1/work-items", "/api/v1/secretariat", "/api/v1/juiz", "/api/v1/admin", "/api/v1/jobs")),
    COMUNICACAO(List.of("/api/v1/chat", "/api/v1/comunicacao", "/api/v1/notifications", "/api/v1/live")),
    INTEGRACOES(List.of("/api/v1/integrations", "/api/v1/connectors", "/api/v1/gov", "/api/v1/cnj")),
    CORE(List.of("/"));

    private final List<String> prefixes;

    PjbFunctionalDomain(List<String> prefixes) {
        this.prefixes = prefixes;
    }

    public List<String> prefixes() {
        return prefixes;
    }

    public static PjbFunctionalDomain fromUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return CORE;
        }
        for (PjbFunctionalDomain domain : values()) {
            if (domain == CORE) {
                continue;
            }
            for (String prefix : domain.prefixes) {
                if (prefix != null && !prefix.isBlank() && uri.startsWith(prefix)) {
                    return domain;
                }
            }
        }
        return CORE;
    }

    public String externalName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
