package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

import java.util.Objects;

public record EmbargosDeclaracaoOmissao(
        String pontoOmitido,
        boolean potencialEfeitoModificativo) implements EmbargosDeclaracaoGround {

    public EmbargosDeclaracaoOmissao {
        Objects.requireNonNull(pontoOmitido, "pontoOmitido");
        if (pontoOmitido.isBlank()) {
            throw new IllegalArgumentException("Embargos por omissão exigem descrição do ponto omitido");
        }
    }

    @Override
    public String code() {
        return "OMISSAO";
    }

    @Override
    public String formalName() {
        return "Omissão";
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
