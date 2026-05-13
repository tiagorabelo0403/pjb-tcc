package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

public record RecursalMeshConsistencyView(
        String recursoId,
        Long processoId,
        String speciesCode,
        String profileName,
        RecursalLifecycleState snapshotState,
        int snapshotRevision,
        FaseProcessual faseProcessual,
        StatusProcesso statusProcesso,
        boolean projectionConsistent,
        boolean ledgerConsistent,
        boolean processConsistent,
        boolean routePlanConsistent,
        boolean aggregateFingerprintConsistent,
        boolean projectionFingerprintConsistent,
        boolean ledgerFingerprintConsistent,
        boolean overallConsistent,
        int ledgerEntries,
        Integer ledgerLastRevision,
        RecursalTransitionEvent ledgerLastEvent,
        RecursalLifecycleState ledgerLastState,
        Instant checkedAt,
        List<String> inconsistencies) {
}
