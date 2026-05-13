package com.tcc.pjb.backend.model.entity.enums;


public enum TipoAudiencia {

    
    
    
    CONCILIACAO,
    MEDIACAO,
    INSTRUCAO,
    JUSTIFICACAO,

    CUSTODIA,
    INTERROGATORIO,
    JURI,

    UNA_TRABALHO,
    PUBLICA_AMBIENTAL,

    OITIVA_TESTEMUNHA,
    SUSTENTACAO_ORAL,
    JULGAMENTO_COLEGIADO,
    SESSAO_ADMINISTRATIVA,

    
    
    
    AUDIENCIA_CONCILIACAO,
    AUDIENCIA_MEDIACAO,
    AUDIENCIA_INSTRUCAO_E_JULGAMENTO,
    AUDIENCIA_JUSTIFICACAO,

    AUDIENCIA_CUSTODIA,
    AUDIENCIA_INTERROGATORIO,
    AUDIENCIA_JURI,

    AUDIENCIA_UNA_TRABALHO,
    AUDIENCIA_PUBLICA_AMBIENTAL;

    
    public TipoAudiencia normalizar() {
        return switch (this) {
            case AUDIENCIA_CONCILIACAO -> CONCILIACAO;
            case AUDIENCIA_MEDIACAO -> MEDIACAO;
            case AUDIENCIA_INSTRUCAO_E_JULGAMENTO -> INSTRUCAO;
            case AUDIENCIA_JUSTIFICACAO -> JUSTIFICACAO;

            case AUDIENCIA_CUSTODIA -> CUSTODIA;
            case AUDIENCIA_INTERROGATORIO -> INTERROGATORIO;
            case AUDIENCIA_JURI -> JURI;

            case AUDIENCIA_UNA_TRABALHO -> UNA_TRABALHO;
            case AUDIENCIA_PUBLICA_AMBIENTAL -> PUBLICA_AMBIENTAL;

            default -> this;
        };
    }

    
    public String descricao() {
        return switch (this.normalizar()) {
            case CONCILIACAO -> "Audiência de Conciliação";
            case MEDIACAO -> "Audiência de Mediação";
            case INSTRUCAO -> "Audiência de Instrução e Julgamento";
            case JUSTIFICACAO -> "Audiência de Justificação";

            case CUSTODIA -> "Audiência de Custódia";
            case INTERROGATORIO -> "Audiência de Interrogatório";
            case JURI -> "Audiência do Tribunal do Júri";

            case UNA_TRABALHO -> "Audiência Una (Trabalho)";
            case PUBLICA_AMBIENTAL -> "Audiência Pública Ambiental";

            case OITIVA_TESTEMUNHA -> "Oitiva de Testemunha";
            case SUSTENTACAO_ORAL -> "Sustentação Oral";
            case JULGAMENTO_COLEGIADO -> "Julgamento Colegiado";
            case SESSAO_ADMINISTRATIVA -> "Sessão Administrativa";

            
            
            default -> "Audiência (" + this.normalizar().name().replace('_', ' ') + ")";
        };
    }
}
