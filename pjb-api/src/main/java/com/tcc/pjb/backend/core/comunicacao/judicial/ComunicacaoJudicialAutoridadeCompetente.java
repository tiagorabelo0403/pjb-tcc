package com.tcc.pjb.backend.core.comunicacao.judicial;

public enum ComunicacaoJudicialAutoridadeCompetente {
    MAGISTRADO,
    SECRETARIA_JUDICIAL,
    OFICIAL_JUSTICA,
    RELATOR_TRIBUNAL,
    ORGAO_COLEGIADO_TRIBUNAL,
    PRESIDENCIA_TRIBUNAL,
    APOIO_TRIBUNAL,
    REPRESENTACAO_PROCESSUAL,
    COOPERACAO_INTERNACIONAL,
    INDISPONIVEL;

    public boolean isTribunal() {
        return this == RELATOR_TRIBUNAL || this == ORGAO_COLEGIADO_TRIBUNAL || this == PRESIDENCIA_TRIBUNAL || this == APOIO_TRIBUNAL;
    }
}
