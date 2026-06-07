package com.tcc.pjb.backend.core.security.identity;

import java.util.Objects;

public record AssinaturaInvalida(
        MotivoAssinaturaInvalida motivo,
        String algoritmo
) implements ResultadoVerificacaoAssinatura {

    public AssinaturaInvalida {
        Objects.requireNonNull(motivo);
        Objects.requireNonNull(algoritmo);
    }
}
