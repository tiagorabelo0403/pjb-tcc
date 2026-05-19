package com.tcc.pjb.backend.model.entity.enums;

public enum FuncaoServidorJudiciario {

    DIRETOR_SECRETARIA("Diretor de Secretaria", true, true, true, true, true),
    CHEFE_CARTORIO("Chefe de Cartório", true, true, true, true, true),
    OFICIAL_MAIOR("Oficial Maior", false, true, true, true, false),
    ESCRIVAO_JUDICIAL("Escrivão Judicial", false, false, true, false, false),
    ANALISTA_JUDICIARIO("Analista Judiciário", false, true, true, true, false),
    TECNICO_JUDICIARIO("Técnico Judiciário", false, false, true, false, false),
    ASSISTENTE_JUDICIARIO("Assistente Judiciário", false, false, false, false, false),
    CALCULISTA_JUDICIAL("Calculista Judicial", false, false, false, false, false),
    OFICIAL_JUSTICA_AVALIADOR("Oficial de Justiça Avaliador", false, false, false, false, false),
    CONCILIADOR_SESSAO("Conciliador de Sessão CEJUSC", false, false, false, false, false);

    private final String label;
    private final boolean podeProferir;
    private final boolean podeConcluir;
    private final boolean podeIntimar;
    private final boolean podeDistribuir;
    private final boolean podeArquivar;

    FuncaoServidorJudiciario(String label, boolean podeProferir, boolean podeConcluir,
                              boolean podeIntimar, boolean podeDistribuir, boolean podeArquivar) {
        this.label = label;
        this.podeProferir = podeProferir;
        this.podeConcluir = podeConcluir;
        this.podeIntimar = podeIntimar;
        this.podeDistribuir = podeDistribuir;
        this.podeArquivar = podeArquivar;
    }

    public String label() {
        return label;
    }

    public boolean podeProferir() {
        return podeProferir;
    }

    public boolean podeConcluir() {
        return podeConcluir;
    }

    public boolean podeIntimar() {
        return podeIntimar;
    }

    public boolean podeDistribuir() {
        return podeDistribuir;
    }

    public boolean podeArquivar() {
        return podeArquivar;
    }
}
