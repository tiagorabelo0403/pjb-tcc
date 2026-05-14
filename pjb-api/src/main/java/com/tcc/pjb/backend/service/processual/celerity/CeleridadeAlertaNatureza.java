package com.tcc.pjb.backend.service.processual.celerity;

public enum CeleridadeAlertaNatureza {

    PRAZO_PROCESSUAL("Prazo processual previsto no CPC/CPP/CLT em risco"),
    PRAZO_LEGAL("Prazo legal fixado em norma específica em risco"),
    URGENCIA_CONSTITUCIONAL("Urgência decorrente de garantia constitucional — art. 5º CF/88"),
    RISCO_PRESCRICAO("Risco de extinção da pretensão por prescrição intercorrente"),
    TUTELA_URGENTE_PENDENTE("Tutela de urgência deferida sem cumprimento integral"),
    REU_PRESO_PRAZO("Prazo constitucional de réu preso — celeridade obrigatória"),
    SAUDE_OU_VIDA("Risco imediato à saúde ou à vida de pessoa envolvida"),
    IDOSO_PRIORIDADE("Prioridade legal — parte com 60 anos ou mais (Estatuto do Idoso)"),
    CRIANCA_ADOLESCENTE("Prioridade absoluta — criança ou adolescente envolvido (ECA)"),
    ALIMENTOS("Processo de alimentos — caráter alimentar urgente");

    private final String descricao;

    CeleridadeAlertaNatureza(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }

    public boolean eUrgenciaConstitucional() {
        return switch (this) {
            case URGENCIA_CONSTITUCIONAL, REU_PRESO_PRAZO,
                 SAUDE_OU_VIDA, CRIANCA_ADOLESCENTE -> true;
            default -> false;
        };
    }

    public CeleridadeNivel nivelMinimo() {
        return switch (this) {
            case SAUDE_OU_VIDA, REU_PRESO_PRAZO -> CeleridadeNivel.CRITICO;
            case URGENCIA_CONSTITUCIONAL, TUTELA_URGENTE_PENDENTE,
                 CRIANCA_ADOLESCENTE, ALIMENTOS -> CeleridadeNivel.URGENTE;
            case RISCO_PRESCRICAO, IDOSO_PRIORIDADE -> CeleridadeNivel.PRIORITARIO;
            default -> CeleridadeNivel.ATENCAO;
        };
    }
}
