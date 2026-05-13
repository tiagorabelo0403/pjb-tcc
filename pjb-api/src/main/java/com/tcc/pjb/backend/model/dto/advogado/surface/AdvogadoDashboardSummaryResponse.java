package com.tcc.pjb.backend.model.dto.advogado.surface;

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
            LocalDateTime dataInicio,
            LocalDateTime dataFim,
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
