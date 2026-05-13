package com.tcc.pjb.backend.core.comunicacao.institucional.routing;

import java.util.ArrayList;
import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional;

public record PlanoEntregaInstitucional(
        CanalEntregaInstitucional canalPrincipal,
        List<CanalEntregaInstitucional> canaisFallback,
        boolean avisoInformativoHabilitado,
        boolean forcarDigital,
        boolean forcarOficial,
        List<String> justificativas
) {
    public PlanoEntregaInstitucional {
        if (canalPrincipal == null) {
            throw new IllegalArgumentException("canalPrincipal é obrigatório");
        }
        ArrayList<CanalEntregaInstitucional> fallback = new ArrayList<>();
        for (CanalEntregaInstitucional canal : PayloadMaps.copyListDistinct(canaisFallback)) {
            if (!canal.equals(canalPrincipal)) {
                fallback.add(canal);
            }
        }
        canaisFallback = fallback.isEmpty() ? List.of() : List.copyOf(fallback);
        justificativas = PayloadMaps.copyDistinctStrings(justificativas);
    }
}
