package com.tcc.pjb.backend.core.dje.domain;

public record DjePartesNotificacaoResult(Long publicacaoId,
                                         boolean success,
                                         String detail) {
    public static DjePartesNotificacaoResult success(Long publicacaoId, String detail) {
        return new DjePartesNotificacaoResult(publicacaoId, true, detail);
    }

    public static DjePartesNotificacaoResult failed(Long publicacaoId, String detail) {
        return new DjePartesNotificacaoResult(publicacaoId, false, detail);
    }
}
