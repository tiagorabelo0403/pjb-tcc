package com.tcc.pjb.backend.model.dto.cidadao.surface;

import java.util.List;

public record CidadaoOrientacaoAudienciaResponse(
        Long processoId,
        String tribunal,
        String comarca,
        String orientacao,
        List<String> documentosNecessarios,
        String chegar
) {}
