package com.tcc.pjb.backend.model.entity.enums.document;

public enum DocumentoOrigemSistema {
    PJB,
    PJE,
    PJE_2X,
    ESAJ,
    EPROC,
    PROJUDI,
    CRETA,
    MNI,
    PDPJ,
    DIGITALIZACAO,
    UPLOAD_INTERNO;

    public boolean eOriginalPJB() {
        return this == PJB || this == UPLOAD_INTERNO;
    }

    public boolean requerNormalizacao() {
        return this != PJB && this != UPLOAD_INTERNO;
    }
}
