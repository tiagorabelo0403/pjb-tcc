package com.tcc.pjb.backend.service.servidor;

import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalFocusResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SecretariaSnapshot(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        String vara,
        long totalFilaVara,
        List<String> juntadasPendentes,
        List<String> intimacoesExpedir,
        List<String> mandadosExpedir,
        List<String> conclusosPendentes,
        int prazosVencendo24h,
        List<?> prazoRadar,
        Object sessionRisk,
        CalendarInstitutionalFocusResponse institutionalCalendarFocus,
        CalendarInstitutionalBridgeResponse institutionalCalendarBridge,
        Map<String, Object> operationalSignals,
        Map<String, Object> nativeComposition,
        Map<String, Object> collectionComposition,
        Map<String, Object> actionSurface,
        Map<String, Object> executionSurface,
        Map<String, Object> sharedExperience
) {}
