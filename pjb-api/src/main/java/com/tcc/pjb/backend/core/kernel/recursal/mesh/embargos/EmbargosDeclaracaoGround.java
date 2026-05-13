package com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos;

public sealed interface EmbargosDeclaracaoGround permits EmbargosDeclaracaoOmissao, EmbargosDeclaracaoContradicao, EmbargosDeclaracaoObscuridade, EmbargosDeclaracaoErroMaterial {
    EmbargosDeclaracaoGround OMISSAO = new EmbargosDeclaracaoOmissao("PONTO_OMITIDO", false);
    String code();
    default String name() { return code(); }
    String formalName();
    boolean admiteContraditorioPrevio();
    boolean admiteEfeitoModificativo();
}
