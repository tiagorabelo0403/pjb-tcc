package com.tcc.pjb.backend.model.dto.advogado.surface;

import java.util.Map;

public record AdvogadoProdutividadeEscritorioResponse(
        long totalProcessos,
        long processosAtivos,
        long processosEncerrados,
        Map<String, Long> distribuicaoPorStatus,
        Map<String, Long> distribuicaoPorRito,
        Double duracaoMediaDiasProcessosEncerrados
) {}
