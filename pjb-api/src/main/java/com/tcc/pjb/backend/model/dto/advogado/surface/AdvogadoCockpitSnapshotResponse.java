package com.tcc.pjb.backend.model.dto.advogado.surface;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record AdvogadoCockpitSnapshotResponse(
        @Schema(description = "Data/hora de geração do snapshot do cockpit", format = "date-time",
                example = "2026-06-01T10:00:00-03:00") LocalDateTime generatedAt,
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
