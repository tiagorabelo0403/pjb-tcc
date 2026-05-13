package com.tcc.pjb.backend.model.dto.advogado;

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
        private LocalDateTime dataInicio;
        private LocalDateTime dataFim;
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
