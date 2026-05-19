package com.tcc.pjb.backend.modules.acordo.domain;

public enum AcordoSessaoStatus {
    NOT_ELIGIBLE,
    ELIGIBLE,
    INVITED,
    WAITING_PARTICIPANTS,
    OPEN,
    PAUSED,
    PROPOSAL_PENDING,
    COUNTERPROPOSAL_PENDING,
    AGREEMENT_DRAFTED,
    WAITING_SIGNATURES,
    SIGNED,
    SENT_TO_HOMOLOGATION,
    HOMOLOGATED,
    REJECTED_BY_JUDGE,
    FAILED,
    EXPIRED,
    CLOSED;

    public boolean terminal() {
        return this == HOMOLOGATED
                || this == REJECTED_BY_JUDGE
                || this == FAILED
                || this == EXPIRED
                || this == CLOSED
                || this == NOT_ELIGIBLE;
    }

    public boolean acceptsMessages() {
        return this == OPEN
                || this == PROPOSAL_PENDING
                || this == COUNTERPROPOSAL_PENDING
                || this == AGREEMENT_DRAFTED
                || this == WAITING_SIGNATURES
                || this == SIGNED;
    }

    public boolean acceptsProposals() {
        return this == OPEN
                || this == PROPOSAL_PENDING
                || this == COUNTERPROPOSAL_PENDING;
    }
}
