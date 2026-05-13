package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.List;
import java.util.Locale;

public final class RecursalTerminologyCatalog {

    private RecursalTerminologyCatalog() {
    }

    public static String nomenclaturaAtiva(String recurso) {
        String normalized = normalize(recurso);
        return switch (normalized) {
            case "APELACAO" -> "APELANTE/APELADO";
            case "AGRAVO_INTERNO", "AGRAVO_DE_INSTRUMENTO", "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO" -> "AGRAVANTE/AGRAVADO";
            case "RECURSO_INOMINADO", "RECURSO_ESPECIAL", "RECURSO_EXTRAORDINARIO" -> "RECORRENTE/RECORRIDO";
            case "EMBARGOS_DECLARACAO", "EMBARGOS_DIVERGENCIA" -> "EMBARGANTE/EMBARGADO";
            case "PODER_RECORRER_BLOQUEADO", "IRRECORRIVEL" -> "SEM_RELACAO_RECURSAL_ATIVA";
            default -> "RECORRENTE/RECORRIDO";
        };
    }

    public static String verboPrincipal(String recurso) {
        String normalized = normalize(recurso);
        return switch (normalized) {
            case "EMBARGOS_DECLARACAO", "EMBARGOS_DIVERGENCIA" -> "OPOR";
            case "IRRECORRIVEL", "PODER_RECORRER_BLOQUEADO" -> "NAO_INTERPOR";
            default -> "INTERPOR";
        };
    }

    public static List<String> verbosOperacionais(String recurso) {
        String principal = verboPrincipal(recurso);
        if (principal.equals("NAO_INTERPOR")) {
            return List.of("REVISAR", "SUSTAR", "REGISTRAR");
        }
        if (principal.equals("OPOR")) {
            return List.of("OPOR", "APRESENTAR", "OFERECER");
        }
        return List.of("INTERPOR", "APRESENTAR", "OFERECER");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
