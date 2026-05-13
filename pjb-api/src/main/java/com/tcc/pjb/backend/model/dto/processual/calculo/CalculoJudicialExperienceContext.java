package com.tcc.pjb.backend.model.dto.processual.calculo;

public record CalculoJudicialExperienceContext(
        String ramoDireito,
        String classeProcessual,
        String tipoCausa,
        String perfilEquipe,
        String tribunal,
        String sistemaOrigem
) {
}
