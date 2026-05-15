package com.tcc.pjb.backend.service.recuperacaojudicial;

public enum SituacaoRecuperacaoJudicial {
    PEDIDO_PROTOCOLADO,
    DEFERIMENTO_PROCESSAMENTO,
    PLANO_APRESENTADO,
    PLANO_EM_VOTACAO_AG,
    PLANO_APROVADO,
    EM_CUMPRIMENTO,
    ENCERRADA,
    CONVOLADA_EM_FALENCIA;

    public boolean isAtiva() {
        return this == DEFERIMENTO_PROCESSAMENTO
                || this == PLANO_APRESENTADO
                || this == PLANO_EM_VOTACAO_AG
                || this == PLANO_APROVADO
                || this == EM_CUMPRIMENTO;
    }
}
