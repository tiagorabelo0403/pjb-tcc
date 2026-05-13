package com.tcc.pjb.backend.modules.advocacia.office.enums;

public enum OfficeAffiliationInviteStatus {
    PENDING,
    AWAITING_FINAL_APPROVAL,
    ACCEPTED,
    REJECTED,
    REVOKED,
    EXPIRED;

    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED || this == REVOKED || this == EXPIRED;
    }

    public boolean isPendingDecision() {
        return this == PENDING || this == AWAITING_FINAL_APPROVAL;
    }
}
