package com.tcc.pjb.backend.core.security.professional;

public enum ProfessionalGrantApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    REVOKED;

    public String displayName() {
        return switch (this) {
            case PENDING -> "Pendente de aprovação";
            case APPROVED -> "Aprovado";
            case REJECTED -> "Rejeitado";
            case REVOKED -> "Revogado";
        };
    }

    public boolean isTerminal() {
        return this == REJECTED || this == REVOKED;
    }
}
