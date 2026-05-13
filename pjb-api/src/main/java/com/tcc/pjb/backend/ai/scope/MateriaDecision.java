package com.tcc.pjb.backend.ai.scope;

import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;

public record MateriaDecision(
        MateriaJurisdicao materia,
        double confidence,
        List<String> signals
) {
}
