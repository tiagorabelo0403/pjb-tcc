package com.tcc.pjb.backend.model.dto.juiz;

import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalFocusResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record JuizGabineteSnapshotResponse(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        String gabineteKey,
        long acervoTotal,
        int processosSentenciados,
        int processosInstruidos,
        int recursosResponder,
        int prazos24h,
        int prazos48h,
        String loadBand,
        String coordinationMode,
        int urgentItems,
        int blockingItems,
        int recursalItems,
        int hearingItems,
        int secrecyItems,
        List<JuizGabineteFilaItemResponse> filaDecisional,
        List<JuizGabineteFilaItemResponse> minutasPendentes,
        List<JuizGabineteFilaItemResponse> audienciasAgendadas,
        List<String> labels,
        Map<String, Object> metadata,
        CalendarInstitutionalFocusResponse institutionalCalendarFocus,
        CalendarInstitutionalBridgeResponse institutionalCalendarBridge
) {
}
