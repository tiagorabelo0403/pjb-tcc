package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoMeusProcessosResponse(
        LocalDateTime generatedAt,
        int totalProcessos,
        List<RitoSection> ritos,
        String uiLegendUrl,
        AreaLinks links
) {

    public record RitoSection(
            String rito,
            String ritoLabel,
            String ritoTitle,
            String ramoSugerido,
            Double confidenceMedia,
            int total,
            int urgentCount,
            int pendenteCount,
            int recursoCount,
            int resultadoCount,
            int encerradoCount,
            List<CidadaoProcessoCardDto> processos
    ) {}
}
