package com.tcc.pjb.backend.core.comunicacao.institucional.access;

import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;

public record ResultadoAutorizacaoCaixaInstitucional(
        boolean autorizado,
        String unidadeCodigo,
        String caixaCodigo,
        CapacidadeCaixaInstitucional capacidadeSolicitada,
        List<String> justificativas,
        List<VinculoUsuarioCaixaInstitucional> vinculosElegiveis
) {
    public ResultadoAutorizacaoCaixaInstitucional {
        Objects.requireNonNull(unidadeCodigo, "unidadeCodigo");
        Objects.requireNonNull(caixaCodigo, "caixaCodigo");
        Objects.requireNonNull(capacidadeSolicitada, "capacidadeSolicitada");
        justificativas = List.copyOf(justificativas == null ? List.of() : justificativas);
        vinculosElegiveis = List.copyOf(vinculosElegiveis == null ? List.of() : vinculosElegiveis);
    }
}
