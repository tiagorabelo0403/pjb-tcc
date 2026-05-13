package com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain;

import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.Objects;

public record InstitutionalPanelBlueprintSpec(
        String codigo,
        String escopo,
        String panel,
        String audience,
        String titulo,
        String rotaInicial,
        List<String> secoesPrimarias,
        List<String> acoesRapidas,
        List<String> guardasSeguranca,
        List<String> regrasVisibilidade,
        List<String> fundamentos
) {
    public InstitutionalPanelBlueprintSpec {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        Objects.requireNonNull(rotaInicial);
        secoesPrimarias = PayloadMaps.copyListDistinct(secoesPrimarias);
        acoesRapidas = PayloadMaps.copyListDistinct(acoesRapidas);
        guardasSeguranca = PayloadMaps.copyDistinctStrings(guardasSeguranca);
        regrasVisibilidade = PayloadMaps.copyDistinctStrings(regrasVisibilidade);
        fundamentos = PayloadMaps.copyDistinctStrings(fundamentos);
    }
}
