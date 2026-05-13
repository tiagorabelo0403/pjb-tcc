package com.tcc.pjb.backend.core.processo.plantao.domain;

import java.util.List;

public record ProcessoResponsabilidadeOperacional(
        String lane,
        String descricao,
        String regime,
        boolean titularObrigatorio,
        boolean stepUpObrigatorio,
        List<String> guardas,
        List<String> canaisPermitidos
) {
    public ProcessoResponsabilidadeOperacional {
        lane = lane == null || lane.isBlank() ? "ROTINA" : lane;
        descricao = descricao == null ? "" : descricao;
        regime = regime == null || regime.isBlank() ? lane : regime;
        guardas = guardas == null ? List.of() : List.copyOf(guardas);
        canaisPermitidos = canaisPermitidos == null ? List.of() : List.copyOf(canaisPermitidos);
    }
}
