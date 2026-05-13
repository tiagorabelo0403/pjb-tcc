package com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain;

import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import java.util.Objects;

public final class InstitutionalTopologyKeys {

    private InstitutionalTopologyKeys() {
    }

    public static String queueKey(String unidadeCodigo, String caixaCodigo) {
        return normalizeRequired(unidadeCodigo, "unidadeCodigo") + "|" + normalizeRequired(caixaCodigo, "caixaCodigo");
    }

    public static String unitRecipientKey(String unidadeCodigo, DestinatarioInstitucionalKind destinatarioKind) {
        Objects.requireNonNull(destinatarioKind, "destinatarioKind");
        return normalizeRequired(unidadeCodigo, "unidadeCodigo") + "|" + destinatarioKind.name();
    }

    public static boolean matchesQueue(String unidadeCodigoA, String caixaCodigoA, String unidadeCodigoB, String caixaCodigoB) {
        return queueKey(unidadeCodigoA, caixaCodigoA).equals(queueKey(unidadeCodigoB, caixaCodigoB));
    }

    public static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeRequired(String value, String field) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " obrigatorio");
        }
        return normalized;
    }
}
