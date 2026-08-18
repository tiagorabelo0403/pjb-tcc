package com.tcc.pjb.backend.model.dto.jurisprudencia;

import io.swagger.v3.oas.annotations.media.Schema;
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
            @Schema(description = "Data de publicação do precedente", format = "date",
                    example = "2026-06-01") String dataPublicacao,
            String urlReferencia,
            String ramoSugerido,
            String ritoSugerido
    ) {
    }
}
