package com.tcc.pjb.backend.core.processo.prazo.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoPrazoCienciaProfile(
        String modoCiencia,
        boolean cienciaPessoalObrigatoria,
        boolean contagemEmDiasUteis,
        boolean prazoEmDobro,
        boolean exigeConfirmacaoExpressa,
        List<String> guardas,
        List<String> fundamentos
) {
    public ProcessoPrazoCienciaProfile {
        Objects.requireNonNull(modoCiencia);
        guardas = guardas == null ? List.of() : List.copyOf(guardas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
