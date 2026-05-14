package com.tcc.pjb.backend.service.prazo;

public enum PrazoSituacao {
    ABERTO,
    SUSPENSO,
    INTERROMPIDO,
    ENCERRADO,
    DECURSO_CERTIFICADO,
    PRORROGADO,
    SOBRESTADO;

    public boolean eAtivo() {
        return this == ABERTO || this == PRORROGADO;
    }

    public boolean eTerminado() {
        return this == ENCERRADO || this == DECURSO_CERTIFICADO;
    }
}
