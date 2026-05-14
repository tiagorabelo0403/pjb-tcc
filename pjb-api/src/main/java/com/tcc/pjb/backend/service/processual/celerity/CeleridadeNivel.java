package com.tcc.pjb.backend.service.processual.celerity;

public enum CeleridadeNivel {

    NORMAL(0, "Processo dentro do prazo esperado"),
    ATENCAO(1, "Processo aproximando-se do limite de SLA"),
    PRIORITARIO(2, "Processo com SLA vencido — prioridade operacional"),
    URGENTE(3, "Processo com urgência legal ou constitucional"),
    CRITICO(4, "Processo em risco grave — réu preso, tutela pendente ou saúde/vida");

    private final int peso;
    private final String descricao;

    CeleridadeNivel(int peso, String descricao) {
        this.peso = peso;
        this.descricao = descricao;
    }

    public int getPeso() { return peso; }
    public String getDescricao() { return descricao; }

    public boolean superiorA(CeleridadeNivel outro) {
        return this.peso > outro.peso;
    }

    public boolean requerAcaoImediata() {
        return this == URGENTE || this == CRITICO;
    }
}
