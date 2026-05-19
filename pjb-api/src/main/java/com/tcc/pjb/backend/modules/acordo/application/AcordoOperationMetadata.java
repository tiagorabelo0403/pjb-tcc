package com.tcc.pjb.backend.modules.acordo.application;

public record AcordoOperationMetadata(
        String ipHash,
        String userAgentHash
) {
    public static AcordoOperationMetadata empty() {
        return new AcordoOperationMetadata(null, null);
    }
}
