package com.tcc.pjb.backend.core.security.identity;

public enum MotivoAssinaturaInvalida {
    ASSINATURA_NAO_CONFERE,
    DADOS_INVALIDOS,
    ALGORITMO_FRACO,
    ALGORITMO_NAO_PERMITIDO,
    ALGORITMO_INCOMPATIVEL_COM_CHAVE,
    FALHA_CRIPTOGRAFICA
}
