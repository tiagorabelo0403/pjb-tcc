package com.tcc.pjb.backend.core.governance.fonte.domain;

public enum FonteSoberanaConfiabilidade {
    OFICIAL(100),
    DERIVADA_VERIFICADA(85),
    DERIVADA(70),
    CACHEADA(55),
    LEGADA(40),
    PROVISORIA(20);

    private final int score;

    FonteSoberanaConfiabilidade(int score) {
        this.score = score;
    }

    public int score() {
        return score;
    }
}
