package com.tcc.pjb.backend.modules.advocacia.office.enums;

public enum OfficeProcessTransferStatus {
    PENDING_DESTINATION_ACCEPTANCE,
    EXECUTED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == EXECUTED || this == REJECTED || this == CANCELLED;
    }
}
