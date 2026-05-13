package com.tcc.pjb.backend.model.dto.intelligence;

import java.util.List;

public record StructuredProcessSummaryResponse(
        Long processoId,
        String numeroProcesso,
        String quickSummary,
        int estimatedReadingSeconds,
        List<String> partes,
        List<String> pedidos,
        List<String> provasPrincipais,
        List<String> decisoesAnteriores,
        List<String> estadoAtual,
        List<String> proximosAtos,
        List<String> alertas,
        List<String> fontes
) {
}
