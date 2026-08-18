package com.tcc.pjb.backend.model.entity.enums;

public enum StatusCiencia {

    PENDENTE("Pendente", false),
    CONFIRMADA("Confirmada", true),
    FICTA_CONFIRMADA("Ficta — art. 231 CPC", true),
    REJEITADA("Rejeitada", true),
    EXPIRADA("Expirada", true),
    CANCELADA("Cancelada", true);

    private final String label;
    private final boolean terminal;

    StatusCiencia(String label, boolean terminal) {
        this.label = label;
        this.terminal = terminal;
    }

    public String label() { return label; }
    public boolean isTerminal() { return terminal; }

    public boolean permiteTransicaoPara(StatusCiencia novo) {
        if (this.terminal) return false;
        return novo == CONFIRMADA || novo == FICTA_CONFIRMADA
                || novo == CANCELADA || novo == EXPIRADA;
    }
}
