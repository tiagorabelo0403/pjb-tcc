package com.tcc.pjb.backend.service.defensor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DefensoriaSnapshot(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        String nucleo,
        List<String> assistidosPendentes,
        List<String> peticoesPendentes,
        List<String> audienciasPendentes,
        List<String> recursosUrgentes,
        int prazosVencendo24h,
        int presosEmAcompanhamento,
        List<?> prazoRadar,
        Object sessionRisk,
        Map<String, Object> operationalSignals,
        Map<String, Object> nativeComposition,
        Map<String, Object> collectionComposition,
        Map<String, Object> actionSurface,
        Map<String, Object> executionSurface,
        Map<String, Object> sharedExperience
) {}
