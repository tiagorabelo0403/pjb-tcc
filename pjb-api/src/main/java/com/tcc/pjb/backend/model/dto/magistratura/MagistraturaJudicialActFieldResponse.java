package com.tcc.pjb.backend.model.dto.magistratura;

public record MagistraturaJudicialActFieldResponse(
        String name,
        String label,
        String kind,
        boolean required,
        String sample
) {
}
