package com.tcc.pjb.backend.core.security.identity;

import com.tcc.pjb.backend.model.entity.Usuario;
import java.util.Objects;

public record IdentidadeResolvida(Usuario usuario) implements IdentidadeResolucao {
    public IdentidadeResolvida {
        Objects.requireNonNull(usuario, "usuario");
    }
}
