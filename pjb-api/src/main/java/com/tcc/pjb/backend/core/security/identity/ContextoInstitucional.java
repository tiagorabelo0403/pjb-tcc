package com.tcc.pjb.backend.core.security.identity;

import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import java.util.Objects;

public record ContextoInstitucional(UnidadeInstituicao unidade, String papelNaUnidade) {

    public ContextoInstitucional {
        Objects.requireNonNull(unidade);
        Objects.requireNonNull(papelNaUnidade);
    }
}
