package com.tcc.pjb.backend.service.secretariat.ingest;

public enum SistemaProcessualOrigem {
    PJB,
    PJE,
    PJE_2X,
    ESAJ,
    EPROC,
    PROJUDI,
    CRETA,
    MNI,
    PDPJ,
    OUTRO;

    public boolean requerNormalizacao() {
        return this != PJB;
    }

    public String descricao() {
        return switch (this) {
            case PJB -> "Sistema PJB (nativo)";
            case PJE -> "PJe 1.x";
            case PJE_2X -> "PJe 2.x";
            case ESAJ -> "e-SAJ (TJ-SP)";
            case EPROC -> "eProc (TRF4/TRF5)";
            case PROJUDI -> "Projudi";
            case CRETA -> "Creta";
            case MNI -> "MNI — Modelo Nacional de Interoperabilidade";
            case PDPJ -> "PDPJ — Plataforma Digital do Poder Judiciário";
            case OUTRO -> "Sistema externo não identificado";
        };
    }
}
