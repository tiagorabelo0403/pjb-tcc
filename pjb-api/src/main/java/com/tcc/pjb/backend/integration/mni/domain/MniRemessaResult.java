package com.tcc.pjb.backend.integration.mni.domain;

public record MniRemessaResult(
        boolean success,
        boolean alreadyConfirmed,
        String protocoloDestino,
        String failureReason
) {
    public static MniRemessaResult success(String protocoloDestino) {
        return new MniRemessaResult(true, false, protocoloDestino, null);
    }

    public static MniRemessaResult alreadyConfirmed(String protocoloDestino) {
        return new MniRemessaResult(true, true, protocoloDestino, null);
    }

    public static MniRemessaResult failed(String failureReason) {
        return new MniRemessaResult(false, false, null, failureReason);
    }
}
