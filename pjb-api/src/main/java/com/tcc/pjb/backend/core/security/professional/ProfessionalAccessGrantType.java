package com.tcc.pjb.backend.core.security.professional;

public enum ProfessionalAccessGrantType {
    DESIGNACAO_PROCESSO,
    DESIGNACAO_TERRITORIAL,
    REPRESENTACAO_PROCESSO,
    REPRESENTACAO_ENTE,
    LOTACAO_UNIDADE,
    RELATORIA_PROCESSO,
    COMPOSICAO_COLEGIADO,
    SUBSTITUICAO,
    PLANTAO,
    AUXILIO_JURISDICIONAL,
    DELEGACAO_GABINETE;

    public static final ProfessionalAccessGrantType DESIGNACAO_PROCESSUAL = DESIGNACAO_PROCESSO;
    public static final ProfessionalAccessGrantType REPRESENTACAO_PROCESSUAL = REPRESENTACAO_PROCESSO;


    public String displayName() {
        return switch (this) {
            case DESIGNACAO_PROCESSO -> "Designação processual";
            case DESIGNACAO_TERRITORIAL -> "Designação territorial";
            case REPRESENTACAO_PROCESSO -> "Representação processual";
            case REPRESENTACAO_ENTE -> "Representação institucional do ente";
            case LOTACAO_UNIDADE -> "Lotação em unidade";
            case RELATORIA_PROCESSO -> "Relatoria ativa";
            case COMPOSICAO_COLEGIADO -> "Composição colegiada";
            case SUBSTITUICAO -> "Substituição jurisdicional";
            case PLANTAO -> "Plantão jurisdicional";
            case AUXILIO_JURISDICIONAL -> "Auxílio jurisdicional";
            case DELEGACAO_GABINETE -> "Delegação formal de gabinete";
        };
    }
}
