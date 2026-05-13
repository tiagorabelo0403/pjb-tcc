package com.tcc.pjb.backend.model.dto.intelligence;

public enum PessoaLocalizacaoFundamento {
    CUMPRIMENTO_MANDADO(true, true, false),
    EXECUCAO_PENHORA(true, true, false),
    BUSCA_E_APREENSAO(true, true, false),
    INVESTIGACAO_POLICIAL_FORMAL(true, true, true),
    DECISAO_JUDICIAL_EXECUTIVA(true, true, true),
    COOPERACAO_JUDICIARIA(true, true, true),
    GESTAO_GABINETE_E_INTELIGENCIA_DECISORIA(false, false, true),
    LOCALIZACAO_TESTEMUNHA(true, false, false),
    PREPARACAO_AUDIENCIA(false, false, true);

    private final boolean recomendaContextoFormal;
    private final boolean permiteEnderecoEstrito;
    private final boolean permiteConsultaAberta;

    PessoaLocalizacaoFundamento(boolean recomendaContextoFormal,
                                boolean permiteEnderecoEstrito,
                                boolean permiteConsultaAberta) {
        this.recomendaContextoFormal = recomendaContextoFormal;
        this.permiteEnderecoEstrito = permiteEnderecoEstrito;
        this.permiteConsultaAberta = permiteConsultaAberta;
    }

    public boolean recomendaContextoFormal() {
        return recomendaContextoFormal;
    }

    public boolean permiteEnderecoEstrito() {
        return permiteEnderecoEstrito;
    }

    public boolean permiteConsultaAberta() {
        return permiteConsultaAberta;
    }
}
