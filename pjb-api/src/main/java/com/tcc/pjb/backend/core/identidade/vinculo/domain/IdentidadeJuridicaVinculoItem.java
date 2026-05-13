package com.tcc.pjb.backend.core.identidade.vinculo.domain;

import com.tcc.pjb.backend.core.identidade.resolucao.domain.IdentidadeJuridicaResolucaoItem;
import java.util.List;
import java.util.Objects;

public record IdentidadeJuridicaVinculoItem(
        String codigo,
        IdentidadeJuridicaVinculoParte parte,
        IdentidadeJuridicaResolucaoItem resolucao,
        List<String> processosCorrelatos,
        List<String> alertas,
        List<String> fundamentos
) {
    public IdentidadeJuridicaVinculoItem {
        codigo = Objects.toString(codigo, "").trim();
        Objects.requireNonNull(parte, "parte");
        Objects.requireNonNull(resolucao, "resolucao");
        processosCorrelatos = processosCorrelatos == null ? List.of() : List.copyOf(processosCorrelatos);
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
