package com.tcc.pjb.backend.model.dto.jurisprudencia;

import java.util.List;
import java.util.Map;

public record PrecedentFoundationResponse(
        Long processoId,
        String numeroProcesso,
        String queryEfetiva,
        String ramo,
        String rito,
        long totalResultados,
        Map<String, Long> porFonte,
        Map<String, Long> porTipo,
        List<Item> precedentes,
        List<String> fundamentos
) {
    public PrecedentFoundationResponse {
        porFonte = porFonte == null ? Map.of() : Map.copyOf(porFonte);
        porTipo = porTipo == null ? Map.of() : Map.copyOf(porTipo);
        precedentes = precedentes == null ? List.of() : List.copyOf(precedentes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }

    public record Item(
            Long id,
            String fonte,
            String tipo,
            String identificador,
            String titulo,
            String tese,
            String ementaResumo,
            String dataPublicacao,
            String urlReferencia,
            String ramoSugerido,
            String ritoSugerido
    ) {
    }
}
