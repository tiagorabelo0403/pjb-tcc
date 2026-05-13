package com.tcc.pjb.backend.model.dto.advogado.surface;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record AdvogadoCockpitSnapshotResponse(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        String escritorio,
        long carteiraTotalProcessos,
        List<PrazoCriticoItemResponse> prazosCriticos,
        List<String> intimacoesPendentes,
        List<String> peticoesPendentes,
        List<String> audienciasProximas,
        int intimacoesNaoLidas,
        int recursosVencendo,
        List<?> prazoRadar,
        Object sessionRisk
) {
    public record PrazoCriticoItemResponse(
            Long workItemId,
            String titulo,
            Instant dueAt,
            long horasRestantes,
            String numeroProcesso
    ) {}
}
