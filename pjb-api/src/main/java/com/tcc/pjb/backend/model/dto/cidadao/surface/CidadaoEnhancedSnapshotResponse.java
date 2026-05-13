package com.tcc.pjb.backend.model.dto.cidadao.surface;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import com.tcc.pjb.backend.model.dto.calendar.CalendarPanelResponse;

public record CidadaoEnhancedSnapshotResponse(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        int processosAtivos,
        List<ProcessoResumoCidadaoResponse> timeline,
        List<AcaoPendenteItemResponse> acoesPendentes,
        List<String> audienciasProximas,
        List<?> prazoRadar,
        Object sessionRisk,
        CalendarPanelResponse calendarPanel
) {
    public record ProcessoResumoCidadaoResponse(
            Long id,
            String numero,
            String faseSimples,
            String tipoSimples,
            LocalDateTime dataInicio,
            String tribunal,
            String comarca
    ) {}

    public record AcaoPendenteItemResponse(
            Long workItemId,
            String descricaoSimples,
            Instant dueAt,
            Long diasRestantes
    ) {}
}
