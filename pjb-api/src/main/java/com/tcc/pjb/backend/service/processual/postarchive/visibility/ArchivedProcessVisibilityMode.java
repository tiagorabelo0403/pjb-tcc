package com.tcc.pjb.backend.service.processual.postarchive.visibility;

public enum ArchivedProcessVisibilityMode {
    VISIBLE,
    CONCEALED_PARTY_GATE,
    CONCEALED_SENSITIVE_GATE,
    CONCEALED_INSTITUTIONAL_GATE;

    public boolean requiresControlledAccess() {
        return this != VISIBLE;
    }
}
