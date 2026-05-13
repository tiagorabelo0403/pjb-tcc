package com.tcc.pjb.backend.core.security.professional;

public enum ProfessionalGrantEventType {
    REQUESTED,
    APPROVED,
    REJECTED,
    REVOKED;

    public String displayName() {
        return switch (this) {
            case REQUESTED -> "Solicitação emitida";
            case APPROVED -> "Solicitação aprovada";
            case REJECTED -> "Solicitação rejeitada";
            case REVOKED -> "Grant revogado";
        };
    }
}
