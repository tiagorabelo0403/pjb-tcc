package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

import java.util.Objects;

public record EmbargosDeclaracaoObscuridade(
        String trechoObscuro) implements EmbargosDeclaracaoGround {

    public EmbargosDeclaracaoObscuridade {
        Objects.requireNonNull(trechoObscuro, "trechoObscuro");
        if (trechoObscuro.isBlank()) {
            throw new IllegalArgumentException("Embargos por obscuridade exigem indicação do trecho afetado");
        }
    }

    @Override
    public String code() {
        return "OBSCURIDADE";
    }

    @Override
    public String formalName() {
        return "Obscuridade";
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
