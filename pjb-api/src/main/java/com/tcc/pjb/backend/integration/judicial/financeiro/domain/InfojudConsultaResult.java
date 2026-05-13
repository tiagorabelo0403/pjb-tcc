package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record InfojudConsultaResult(Long consultaId,
                                    boolean success,
                                    String protocolo,
                                    String detail) {
    public static InfojudConsultaResult success(Long id, String protocolo, String detail) {
        return new InfojudConsultaResult(id, true, protocolo, detail);
    }

    public static InfojudConsultaResult failed(Long id, String detail) {
        return new InfojudConsultaResult(id, false, null, detail);
    }
}
