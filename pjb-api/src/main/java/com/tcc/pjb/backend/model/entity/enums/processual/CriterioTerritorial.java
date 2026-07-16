package com.tcc.pjb.backend.model.entity.enums.processual;

public enum CriterioTerritorial {
    DOMICILIO_REU("CPC, art. 46"),
    SITUACAO_DA_COISA("CPC, art. 47"),
    DOMICILIO_AUTOR_HERANCA("CPC, art. 48"),
    DOMICILIO_ALIMENTANDO("CPC, art. 53, II"),
    LOCAL_PRESTACAO_SERVICO("CLT, art. 651"),
    LOCAL_DO_FATO("CPP, art. 70");

    private final String fundamentoLegal;

    CriterioTerritorial(String fundamentoLegal) {
        this.fundamentoLegal = fundamentoLegal;
    }

    public String fundamentoLegal() {
        return fundamentoLegal;
    }
}
