package com.tcc.pjb.backend.model.dto.timeline;

import java.time.Instant;

public record TimelineItemResponse(
        Long id,
        Instant data,
        String faseDe,
        String fasePara,
        String descricao,
        Long atorId,
        String atorNome,
        boolean gerouPrazo,
        boolean consumiuPrazo,
        long prazoPrevistoDias,
        long prazoConsumidoDias,
        String prazoStatus,
        long diasParado,
        String causaProvavelParada,
        Instant proximaJanelaTeorica,
        boolean bloqueioOperacional,
        Instant deadlineOperacionalAberto
) {
}
