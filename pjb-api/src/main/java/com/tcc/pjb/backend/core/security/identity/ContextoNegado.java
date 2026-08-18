package com.tcc.pjb.backend.core.security.identity;

import java.util.Objects;

public record ContextoNegado(MotivoContexto motivo) implements ContextoResolucao {

    public ContextoNegado {
        Objects.requireNonNull(motivo);
    }
}
