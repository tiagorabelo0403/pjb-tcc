package com.tcc.pjb.backend.core.security.professional;

public enum ProfessionalAccessBasis {
    PUBLICO_QUALIFICADO_ADVOCACIA,
    PUBLICO_QUALIFICADO_DEFENSORIA,
    PUBLICO_QUALIFICADO_PROCURADORIA,
    MAGISTRATURA_PUBLICO_AMPLIADO,
    MAGISTRATURA_COMPETENCIA_TERRITORIAL,
    MAGISTRATURA_RELATORIA_ATIVA,
    MAGISTRATURA_COLEGIADO_ATIVO,
    MAGISTRATURA_SUBSTITUICAO_ATIVA,
    MAGISTRATURA_PLANTAO_ATIVO,
    ADVOGADO_PROCURACAO_ATIVA,
    ADVOGADO_CREDENCIAL_SIGILO,
    ADVOGADO_TITULAR_PROCESSO,
    DEFENSORIA_ATUACAO_TERRITORIAL,
    DEFENSORIA_DESIGNACAO_FORMAL,
    PROCURADORIA_ATUACAO_TERRITORIAL,
    PROCURADORIA_REPRESENTACAO_FORMAL,
    APOIO_JUDICIAL_DELEGACAO_FORMAL,
    ADMINISTRADOR_SISTEMA,
    NENHUM;

    public String displayName() {
        return switch (this) {
            case PUBLICO_QUALIFICADO_ADVOCACIA -> "Publicidade qualificada da advocacia";
            case PUBLICO_QUALIFICADO_DEFENSORIA -> "Publicidade qualificada da defensoria";
            case PUBLICO_QUALIFICADO_PROCURADORIA -> "Publicidade qualificada da procuradoria";
            case MAGISTRATURA_PUBLICO_AMPLIADO -> "Leitura pública ampliada da magistratura";
            case MAGISTRATURA_COMPETENCIA_TERRITORIAL -> "Competência jurisdicional territorial";
            case MAGISTRATURA_RELATORIA_ATIVA -> "Relatoria ativa";
            case MAGISTRATURA_COLEGIADO_ATIVO -> "Composição colegiada ativa";
            case MAGISTRATURA_SUBSTITUICAO_ATIVA -> "Substituição jurisdicional ativa";
            case MAGISTRATURA_PLANTAO_ATIVO -> "Plantão jurisdicional ativo";
            case ADVOGADO_PROCURACAO_ATIVA -> "Procuração ativa";
            case ADVOGADO_CREDENCIAL_SIGILO -> "Credencial temporária de sigilo";
            case ADVOGADO_TITULAR_PROCESSO -> "Titularidade direta do processo";
            case DEFENSORIA_ATUACAO_TERRITORIAL -> "Atuação territorial institucional";
            case DEFENSORIA_DESIGNACAO_FORMAL -> "Designação institucional formal";
            case PROCURADORIA_ATUACAO_TERRITORIAL -> "Representação territorial institucional";
            case PROCURADORIA_REPRESENTACAO_FORMAL -> "Representação institucional formal do ente";
            case APOIO_JUDICIAL_DELEGACAO_FORMAL -> "Delegação formal de gabinete";
            case ADMINISTRADOR_SISTEMA -> "Administração sistêmica";
            case NENHUM -> "Sem base válida";
        };
    }
}
