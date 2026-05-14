package com.tcc.pjb.backend.service.publicacao;

public enum MeioPublicacao {
    DIARIO_ELETRONICO_JUSTICA,
    DIARIO_OFICIAL_UNIAO,
    DIARIO_OFICIAL_ESTADO,
    DEJT,
    DOMICILIO_ELETRONICO_JUDICIAL,
    EDITAL_FISICO,
    EDITAL_ELETRONICO;

    public boolean eEletronico() {
        return this != EDITAL_FISICO;
    }

    public boolean requerIntimacaoPessoal() {
        return this == EDITAL_FISICO || this == EDITAL_ELETRONICO;
    }
}
