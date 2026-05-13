package com.tcc.pjb.backend.integration.judicial.financeiro.domain;

public record SisbajudBloqueioResult(Long operacaoId,
                                     IntegracaoJudicialStatus status,
                                     String protocolo,
                                     String detail) {
    public boolean success() {
        return status == IntegracaoJudicialStatus.CONFIRMED;
    }

    public static SisbajudBloqueioResult success(Long id, String protocolo, String detail) {
        return new SisbajudBloqueioResult(id, IntegracaoJudicialStatus.CONFIRMED, protocolo, detail);
    }

    public static SisbajudBloqueioResult failed(Long id, String detail) {
        return new SisbajudBloqueioResult(id, IntegracaoJudicialStatus.FAILED, null, detail);
    }
}
