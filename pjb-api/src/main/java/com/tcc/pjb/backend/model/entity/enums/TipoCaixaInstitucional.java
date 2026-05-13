package com.tcc.pjb.backend.model.entity.enums;

public enum TipoCaixaInstitucional {
    CAIXA_ENTIDADE,
    CAIXA_UNIDADE,
    CAIXA_NUCLEO,
    CAIXA_GABINETE_FUNCIONAL,
    CAIXA_PESSOAL_FUNCIONAL,
    CAIXA_TRIAGEM,
    CAIXA_COORDENACAO,
    CAIXA_SUBSTITUICAO;

    public boolean isCaixaOperacional() {
        return this != CAIXA_ENTIDADE;
    }
}
