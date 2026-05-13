package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.time.Instant;
import java.util.List;

public record RecursalMeshOperationalAlertReport(
        String source,
        RecursalMeshIndexDriftReport drift,
        List<RecursalMeshDashboardBucket> falhasNotificacaoPorCanal,
        List<RecursalMeshDashboardBucket> retryExaustoPorAlvo,
        List<RecursalMeshDashboardBucket> gargalosPorTribunal,
        List<RecursalMeshDashboardBucket> gargalosPorAutoridadeAtual,
        List<RecursalMeshOperationalAlert> alertas,
        Instant generatedAt) {
}
