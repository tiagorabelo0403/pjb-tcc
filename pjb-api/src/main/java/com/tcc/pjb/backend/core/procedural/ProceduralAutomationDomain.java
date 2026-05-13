package com.tcc.pjb.backend.core.procedural;

public enum ProceduralAutomationDomain {
    LOW_SENSITIVITY_CIVIL("Baixa sensibilidade civil"),
    VOLUNTARY_HOMOLOGATORY("Voluntária e homologatória"),
    FAMILY_AND_SUCCESSIONS("Família e sucessões"),
    EXECUTION_AND_ENFORCEMENT("Execução e cumprimento"),
    LABOR_AND_SOCIAL("Trabalho e previdência"),
    TAX_AND_PUBLIC_FINANCE("Tributário e fazenda pública"),
    ADMINISTRATIVE_CONTROL("Administrativo e controle"),
    CONSTITUTIONAL_MANDAMENTAL("Constitucional mandamental"),
    COLLECTIVE_STRUCTURAL("Coletiva e estrutural"),
    PENAL_SENSITIVE("Penal sensível"),
    ELECTORAL_SENSITIVE("Eleitoral sensível"),
    MILITARY_SENSITIVE("Militar sensível"),
    INTERNATIONAL_COOPERATION("Cooperação internacional"),
    HIGH_SECRECY("Alta restrição e sigilo");

    private final String label;

    ProceduralAutomationDomain(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
