package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalSlaPredictiveDashboard(
        List<InstitutionalSlaPredictiveAlert> alertas,
        long totalUnidadesMonitoradas,
        long totalCriticos,
        long totalAltos,
        long totalMedios,
        long totalBaixos,
        Instant generatedAt
) {
}
