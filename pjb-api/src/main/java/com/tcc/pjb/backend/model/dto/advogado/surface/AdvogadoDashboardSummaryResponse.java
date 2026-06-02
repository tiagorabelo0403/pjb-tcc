package com.tcc.pjb.backend.model.dto.advogado.surface;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import com.tcc.pjb.backend.model.dto.calendar.CalendarPanelResponse;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;

public record AdvogadoDashboardSummaryResponse(
        Instant generatedAt,
        long clientesAtivos,
        long clientesArquivados,
        long workItemsOverdue,
        long workItemsDueSoon,
        List<AgendaEventLiteResponse> agendaProxima,
        List<WorkItemLiteResponse> prazosCriticos,
        CalendarPanelResponse calendarPanel
) {
    public record AgendaEventLiteResponse(
            Long id,
            String tipo,
            String titulo,
            @Schema(description = "Data e hora de início do evento da agenda", format = "date-time",
                    example = "2026-06-01T14:00:00-03:00") LocalDateTime dataInicio,
            @Schema(description = "Data e hora de fim do evento da agenda", format = "date-time",
                    example = "2026-06-01T15:00:00-03:00") LocalDateTime dataFim,
            Long processoId,
            String processoNumero
    ) {}

    public record WorkItemLiteResponse(
            Long id,
            Long processoId,
            String processoNumero,
            String titulo,
            Instant dueAt,
            WorkItemStatus status,
            Integer prioridade
    ) {}
}
