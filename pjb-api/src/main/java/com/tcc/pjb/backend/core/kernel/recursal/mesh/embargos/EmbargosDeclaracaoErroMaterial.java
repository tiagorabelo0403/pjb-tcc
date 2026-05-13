package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

import java.util.Objects;

public record EmbargosDeclaracaoErroMaterial(
        String elementoCorrigido,
        boolean erroAritmetico) implements EmbargosDeclaracaoGround {

    public EmbargosDeclaracaoErroMaterial {
        Objects.requireNonNull(elementoCorrigido, "elementoCorrigido");
        if (elementoCorrigido.isBlank()) {
            throw new IllegalArgumentException("Embargos por erro material exigem indicação do elemento a corrigir");
        }
    }

    @Override
    public String code() {
        return erroAritmetico ? "ERRO_MATERIAL_ARITMETICO" : "ERRO_MATERIAL";
    }

    @Override
    public String formalName() {
        return erroAritmetico ? "Erro material aritmético" : "Erro material";
    }

    @Override
    public boolean admiteContraditorioPrevio() {
        return false;
    }

    @Override
    public boolean admiteEfeitoModificativo() {
        return false;
    }
}
