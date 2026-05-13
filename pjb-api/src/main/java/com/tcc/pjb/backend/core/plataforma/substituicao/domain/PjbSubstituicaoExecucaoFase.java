package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

public enum PjbSubstituicaoExecucaoFase {
    RECEPCAO,
    PRECHECK,
    HOMOLOGACAO,
    MIGRACAO_SOMBRA,
    COMUNICACOES,
    CUTOVER,
    ROLLBACK,
    RECONCILIACAO,
    FINALIZACAO
}
