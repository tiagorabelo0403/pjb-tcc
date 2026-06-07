package com.tcc.pjb.backend.core.security.identity;

public sealed interface ResultadoVerificacaoAssinatura
        permits AssinaturaValida, AssinaturaInvalida, ChaveNaoSuportada {
}
