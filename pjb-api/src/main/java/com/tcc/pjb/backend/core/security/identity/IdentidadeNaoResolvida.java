package com.tcc.pjb.backend.core.security.identity;

import java.util.Objects;

public record IdentidadeNaoResolvida(MotivoIdentidade motivo) implements IdentidadeResolucao {
    public IdentidadeNaoResolvida {
        Objects.requireNonNull(motivo, "motivo");
    }
}
