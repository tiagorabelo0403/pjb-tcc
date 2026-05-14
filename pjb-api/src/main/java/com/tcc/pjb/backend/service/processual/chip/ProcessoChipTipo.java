package com.tcc.pjb.backend.service.processual.chip;

public enum ProcessoChipTipo {
    PESSOA_COM_DEFICIENCIA,
    LIBRAS,
    INTERPRETE_GUIA,
    REU_PRESO,
    IDOSO,
    MENOR_ENVOLVIDO,
    ALIMENTOS,
    SAUDE,
    TUTELA_URGENCIA,
    EXECUCAO_PARADA,
    PRAZO_VENCIDO,
    ACORDO_PROVAVEL,
    PERICIA_PENDENTE,
    MANDADO_DEVOLVIDO,
    RITO_SUMARISSIMO,
    PROCESSO_REPETITIVO,
    COMARCA_DO_INTERIOR;

    public boolean geraAlertaUrgente() {
        return this == REU_PRESO || this == TUTELA_URGENCIA
                || this == SAUDE || this == ALIMENTOS || this == MENOR_ENVOLVIDO;
    }

    public boolean requerAcaoCartoraria() {
        return this == MANDADO_DEVOLVIDO || this == EXECUCAO_PARADA
                || this == PRAZO_VENCIDO || this == PERICIA_PENDENTE;
    }

    public String rotulo() {
        return switch (this) {
            case PESSOA_COM_DEFICIENCIA -> "Pessoa com deficiência";
            case LIBRAS -> "Requer intérprete de LIBRAS";
            case INTERPRETE_GUIA -> "Requer intérprete-guia";
            case REU_PRESO -> "Réu preso";
            case IDOSO -> "Parte idosa (prioridade legal)";
            case MENOR_ENVOLVIDO -> "Menor de idade envolvido";
            case ALIMENTOS -> "Alimentos";
            case SAUDE -> "Urgência de saúde";
            case TUTELA_URGENCIA -> "Tutela de urgência pendente";
            case EXECUCAO_PARADA -> "Execução parada";
            case PRAZO_VENCIDO -> "Prazo vencido";
            case ACORDO_PROVAVEL -> "Acordo provável";
            case PERICIA_PENDENTE -> "Perícia pendente";
            case MANDADO_DEVOLVIDO -> "Mandado devolvido";
            case RITO_SUMARISSIMO -> "Rito sumaríssimo";
            case PROCESSO_REPETITIVO -> "Processo repetitivo";
            case COMARCA_DO_INTERIOR -> "Comarca do interior";
        };
    }
}
