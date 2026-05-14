package com.tcc.pjb.backend.service.secretariat.aggregation;

public enum SecretariatAgrupadorTipo {
    JUSTICA_GRATUITA_NAO_APRECIADA,
    TUTELA_LIMINAR_NAO_APRECIADA,
    HABILITACAO_NAO_LIDA,
    DOCUMENTO_NAO_LIDO,
    MANDADO_DEVOLVIDO,
    PETICAO_AVULSA_NAO_LIDA,
    SEGREDO_NAO_APRECIADO,
    PRAZO_CRITICO,
    AUTUACAO_INCONSISTENTE,
    RITO_INCOMPATIVEL_COM_CLASSE,
    PROCESSO_PARADO_ACIMA_SLA,
    EXPEDIENTE_SEM_CIENCIA;

    public boolean eUrgente() {
        return this == JUSTICA_GRATUITA_NAO_APRECIADA
                || this == TUTELA_LIMINAR_NAO_APRECIADA
                || this == PRAZO_CRITICO
                || this == EXPEDIENTE_SEM_CIENCIA;
    }

    public String rotulo() {
        return switch (this) {
            case JUSTICA_GRATUITA_NAO_APRECIADA -> "Justiça gratuita não apreciada";
            case TUTELA_LIMINAR_NAO_APRECIADA -> "Tutela/liminar não apreciada";
            case HABILITACAO_NAO_LIDA -> "Habilitação não lida";
            case DOCUMENTO_NAO_LIDO -> "Documento não lido";
            case MANDADO_DEVOLVIDO -> "Mandado devolvido";
            case PETICAO_AVULSA_NAO_LIDA -> "Petição avulsa não lida";
            case SEGREDO_NAO_APRECIADO -> "Segredo de justiça não apreciado";
            case PRAZO_CRITICO -> "Prazo crítico";
            case AUTUACAO_INCONSISTENTE -> "Autuação inconsistente";
            case RITO_INCOMPATIVEL_COM_CLASSE -> "Rito incompatível com a classe";
            case PROCESSO_PARADO_ACIMA_SLA -> "Processo parado acima do SLA";
            case EXPEDIENTE_SEM_CIENCIA -> "Expediente sem ciência";
        };
    }
}
