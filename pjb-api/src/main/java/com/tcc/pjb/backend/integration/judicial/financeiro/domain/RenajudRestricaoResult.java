package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record RenajudRestricaoResult(Long restricaoId,
                                     boolean success,
                                     String protocolo,
                                     String detail) {
    public static RenajudRestricaoResult success(Long id, String protocolo, String detail) {
        return new RenajudRestricaoResult(id, true, protocolo, detail);
    }

    public static RenajudRestricaoResult failed(Long id, String detail) {
        return new RenajudRestricaoResult(id, false, null, detail);
    }
}
