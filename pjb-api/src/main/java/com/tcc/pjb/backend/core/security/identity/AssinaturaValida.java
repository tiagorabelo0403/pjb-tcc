package com.tcc.pjb.backend.core.security.identity;

import java.util.Objects;

public record AssinaturaValida(String algoritmo) implements ResultadoVerificacaoAssinatura {

    public AssinaturaValida {
        Objects.requireNonNull(algoritmo);
    }
}
