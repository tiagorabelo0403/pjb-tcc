package com.tcc.pjb.backend.core.comunicacao.institucional.access;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaVinculoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;

public record VinculoUsuarioCaixaInstitucional(
        Long usuarioId,
        UnidadeInstitucional unidade,
        CaixaInstitucional caixa,
        FuncaoOperacionalInstitucional funcaoOperacional,
        AbrangenciaVinculoInstitucional abrangencia,
        Set<CapacidadeCaixaInstitucional> capacidades,
        String justificativa
) {
    public VinculoUsuarioCaixaInstitucional {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(unidade, "unidade");
        Objects.requireNonNull(caixa, "caixa");
        Objects.requireNonNull(funcaoOperacional, "funcaoOperacional");
        Objects.requireNonNull(abrangencia, "abrangencia");
        capacidades = capacidades == null || capacidades.isEmpty()
                ? EnumSet.noneOf(CapacidadeCaixaInstitucional.class)
                : EnumSet.copyOf(capacidades);
        justificativa = justificativa == null ? "" : justificativa.trim();
    }

    public boolean permite(CapacidadeCaixaInstitucional capacidade) {
        return capacidade != null && capacidades.contains(capacidade);
    }
}
