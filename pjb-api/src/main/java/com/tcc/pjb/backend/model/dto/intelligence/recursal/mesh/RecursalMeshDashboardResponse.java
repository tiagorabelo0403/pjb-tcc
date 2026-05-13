package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.util.List;

public record RecursalMeshDashboardResponse(
        String source,
        long totalItens,
        long totalSobrestadosPorPrecedente,
        long totalSlaVencido,
        long totalSlaFatalParaPartes,
        long totalPrecedenteAplicado,
        long totalCasoDistinguido,
        List<RecursalMeshDashboardBucket> gargalosPorEstado,
        List<RecursalMeshDashboardBucket> gargalosPorTribunal,
        List<RecursalMeshDashboardBucket> gargalosPorAutoridadeAtual,
        List<RecursalMeshDashboardBucket> porTribunal,
        List<RecursalMeshDashboardBucket> porAutoridadeAtual,
        List<RecursalMeshDashboardBucket> porSlaSeveridade,
        List<RecursalMeshDashboardBucket> porTemaPrecedente) {
}
