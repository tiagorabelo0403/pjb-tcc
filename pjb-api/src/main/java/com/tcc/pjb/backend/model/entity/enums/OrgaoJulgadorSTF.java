package com.tcc.pjb.backend.model.entity.enums;

public enum OrgaoJulgadorSTF {

    PLENARIO("Plenário", 11, 6),
    PRIMEIRA_TURMA("1ª Turma", 5, 3),
    SEGUNDA_TURMA("2ª Turma", 5, 3),
    PRESIDENTE("Presidente", 1, 1);

    private final String label;
    private final int totalMembros;
    private final int quorumMinimo;

    OrgaoJulgadorSTF(String label, int totalMembros, int quorumMinimo) {
        this.label = label;
        this.totalMembros = totalMembros;
        this.quorumMinimo = quorumMinimo;
    }

    public String label() {
        return label;
    }

    public int totalMembros() {
        return totalMembros;
    }

    public int quorumMinimo() {
        return quorumMinimo;
    }

    public boolean atingeQuorum(int presentes) {
        return presentes >= quorumMinimo;
    }

    public boolean exigeReservaPlenario() {
        return this == PLENARIO;
    }

    public int quorumMaioriaAbsoluta() {
        return (totalMembros / 2) + 1;
    }
}
