package com.tcc.pjb.backend.model.dto.govbr;

public record GovBrAssuranceLevelResponse(
        String nivel,
        boolean aptoAtosSensiveis,
        boolean aptoAtosNormais
) {
}
