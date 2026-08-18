package com.tcc.pjb.backend.core.security.identity;

import java.util.Objects;

public record ContextoResolvido(ContextoInstitucional contexto) implements ContextoResolucao {

    public ContextoResolvido {
        Objects.requireNonNull(contexto);
    }
}
