package com.tcc.pjb.backend.core.security.identity;

import java.util.Objects;

public record ChaveNaoSuportada(String algoritmo) implements ResultadoVerificacaoAssinatura {

    public ChaveNaoSuportada {
        Objects.requireNonNull(algoritmo);
    }
}
