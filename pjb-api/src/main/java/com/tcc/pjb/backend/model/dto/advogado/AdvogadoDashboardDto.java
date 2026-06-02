package com.tcc.pjb.backend.model.dto.advogado;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AdvogadoDashboardDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AgendaEventLite {
        private Long id;
        private String tipo;
        private String titulo;
        @Schema(description = "Data e hora de início do evento de agenda", format = "date-time",
                example = "2026-06-01T14:00:00-03:00") private LocalDateTime dataInicio;
        @Schema(description = "Data e hora de fim do evento de agenda", format = "date-time",
                example = "2026-06-01T15:00:00-03:00") private LocalDateTime dataFim;
        private Long processoId;
        private String processoNumero;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WorkItemLite {
        private Long id;
        private Long processoId;
        private String processoNumero;
        private String titulo;
        private Instant dueAt;
        private WorkItemStatus status;
        private Integer prioridade;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SummaryResponse {
        private Instant generatedAt;

        private long clientesAtivos;
        private long clientesArquivados;
        private long workItemsOverdue;
        private long workItemsDueSoon;

        private List<AgendaEventLite> agendaProxima;
        private List<WorkItemLite> prazosCriticos;
    }
}
