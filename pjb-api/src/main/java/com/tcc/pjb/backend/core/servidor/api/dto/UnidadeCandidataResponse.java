package com.tcc.pjb.backend.core.servidor.api.dto;

import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;

public record UnidadeCandidataResponse(
        Long id,
        String codigo,
        String comarca,
        String uf
) {
    public static UnidadeCandidataResponse from(UnidadeJudiciariaCompetencia unidade) {
        return new UnidadeCandidataResponse(unidade.getId(), unidade.getCodigo(), unidade.getComarca(), unidade.getUf());
    }
}
