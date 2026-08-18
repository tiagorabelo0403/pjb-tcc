package com.tcc.pjb.backend.integration.serpro.datavalid;

public class CpfSituacaoBloqueadaException extends RuntimeException {

    private final String codigoPjb;

    CpfSituacaoBloqueadaException(String codigoPjb, String message) {
        super(message);
        this.codigoPjb = codigoPjb;
    }

    public String getCodigoPjb() {
        return codigoPjb;
    }
}
