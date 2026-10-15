package com.tcc.pjb.backend.model.entity.enums.processual;

public enum RegimeDistribuicao {

    ALEATORIO_ALTERNADO("CPC, art. 285", "Sorteio alternado e aleatório entre os juízos competentes no primeiro grau."),
    SORTEIO_RELATORIA("CPC, art. 930", "Sorteio eletrônico de relatoria entre os gabinetes do órgão competente no tribunal."),
    PREVENCAO("CPC, art. 286", "Vinculação ao juízo ou relator já prevento por dependência ou conexão, sem sorteio."),
    EXCLUSAO_LEGAL("CPC, art. 971, parágrafo único", "Sorteio com exclusão objetiva e legal do julgador que atuou na causa de origem."),
    AFETACAO_SOBRESTAMENTO("CPC, arts. 1.036 e 1.037", "Processo sobrestado e vinculado ao relator do tema repetitivo afetado, sem nova distribuição."),
    CRONOLOGICO("CF, art. 100", "Ordenação por ordem cronológica de apresentação, por ente devedor, sem sorteio."),
    DESIGNACAO_LEGAL("Código Eleitoral", "Juízo designado por força de lei, como o juízo eleitoral por zona e biênio."),
    UNIDADE_UNICA("Organização judiciária local", "Vinculação direta por existir uma única unidade competente, sem escolha a sortear.");

    private final String fundamentoLegal;
    private final String descricao;

    RegimeDistribuicao(String fundamentoLegal, String descricao) {
        this.fundamentoLegal = fundamentoLegal;
        this.descricao = descricao;
    }

    public String fundamentoLegal() {
        return fundamentoLegal;
    }

    public String descricao() {
        return descricao;
    }

    public boolean implicaSorteio() {
        return this == ALEATORIO_ALTERNADO || this == SORTEIO_RELATORIA || this == EXCLUSAO_LEGAL;
    }

    public boolean dispensaSorteio() {
        return !implicaSorteio();
    }
}
