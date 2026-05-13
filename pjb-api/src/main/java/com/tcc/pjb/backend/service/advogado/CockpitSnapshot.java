package com.tcc.pjb.backend.service.advogado;

import java.time.LocalDateTime;
import java.util.List;

public record CockpitSnapshot(
        LocalDateTime generatedAt,
        String perfilAtivo,
        String tratamento,
        String escritorio,
        long carteiraTotalProcessos,
        List<PrazoCriticoItem> prazosCriticos,
        List<String> intimacoesPendentes,
        List<String> peticoesPendentes,
        List<String> audienciasProximas,
        int intimacoesNaoLidas,
        int recursosVencendo,
        List<?> prazoRadar,
        Object sessionRisk
) {}
