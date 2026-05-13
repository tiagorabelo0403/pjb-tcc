package com.tcc.pjb.backend.model.entity.enums;

import com.tcc.pjb.backend.core.util.EnumText;

public enum InstitutionalTrustApprovalKind {
    DIRETOR_GERAL,
    MAGISTRADO_REFERENCIAL,
    PJB;

    public static InstitutionalTrustApprovalKind fromTexto(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = EnumText.normalizeToken(raw);
        return switch (normalized) {
            case "DIRETOR_GERAL", "DIRETORIA_FORUM", "DIRETOR", "DIRETOR_GERENCIA" -> DIRETOR_GERAL;
            case "MAGISTRADO_REFERENCIAL", "JUIZ", "MAGISTRADO", "JUIZ_REFERENCIAL" -> MAGISTRADO_REFERENCIAL;
            case "PJB", "PLATAFORMA_PJB", "GOVERNANCA_PJB" -> PJB;
            default -> null;
        };
    }
}
