package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.time.Instant;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;

public record RecursalMeshLedgerView(
        long id,
        String commandId,
        RecursalTransitionEvent eventCode,
        RecursalLifecycleState fromState,
        RecursalLifecycleState toState,
        int fromRevision,
        int toRevision,
        String actor,
        Instant occurredAt) {
}
