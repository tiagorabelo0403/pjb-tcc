package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;

public record RecursalMeshAggregateView(
        String recursoId,
        Long processoId,
        String speciesCode,
        String speciesName,
        String profileName,
        RecursalStateSnapshot snapshot,
        RecursalRoutePlan routePlan,
        RecursalSlaSnapshot sla,
        List<RecursalMeshLedgerView> ledger,
        Instant createdAt,
        Instant updatedAt) {
}
