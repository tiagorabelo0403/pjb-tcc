package com.tcc.pjb.backend.model.dto.processual.recursal.embargos;

public record EmbargosDeclaracaoGroundView(
        String code,
        String formalName,
        boolean admiteContraditorioPrevio,
        boolean admiteEfeitoModificativo) {
}
