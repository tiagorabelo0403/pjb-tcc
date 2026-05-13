package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record PjbSubstituicaoCapacidadeNacional(
        String codigo,
        String titulo,
        Set<PjbSubstituicaoSistemaLegado> sistemasLegados,
        PjbSubstituicaoCapacidadeStatus status,
        String eixoPjb,
        String capacidadeExistente,
        String proximaEntrega
) {
    public PjbSubstituicaoCapacidadeNacional {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        sistemasLegados = sistemasLegados == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(sistemasLegados));
        status = status == null ? PjbSubstituicaoCapacidadeStatus.FALTANTE : status;
        eixoPjb = Objects.toString(eixoPjb, "").trim();
        capacidadeExistente = Objects.toString(capacidadeExistente, "").trim();
        proximaEntrega = Objects.toString(proximaEntrega, "").trim();
    }

    public boolean pendente() {
        return status != PjbSubstituicaoCapacidadeStatus.PRESENTE;
    }
}
