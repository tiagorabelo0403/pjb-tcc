package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

import java.util.Objects;

public record EmbargosDeclaracaoContradicao(
        String eixoContraditorio,
        boolean potencialEfeitoModificativo) implements EmbargosDeclaracaoGround {

    public EmbargosDeclaracaoContradicao {
        Objects.requireNonNull(eixoContraditorio, "eixoContraditorio");
        if (eixoContraditorio.isBlank()) {
            throw new IllegalArgumentException("Embargos por contradição exigem identificação do eixo contraditório");
        }
    }

    @Override
    public String code() {
        return "CONTRADICAO";
    }

    @Override
    public String formalName() {
        return "Contradição";
    }

    @Override
    public boolean admiteContraditorioPrevio() {
        return potencialEfeitoModificativo;
    }

    @Override
    public boolean admiteEfeitoModificativo() {
        return potencialEfeitoModificativo;
    }
}
