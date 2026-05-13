package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.time.Instant;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;

public record RecursalMeshProcessLinkView(
        String recursoId,
        Long processoId,
        String numeroProcesso,
        String speciesCode,
        String profileName,
        RecursalLifecycleState currentState,
        RecursalTribunal tribunalAtual,
        RecursalTribunalDetalhado tribunalDetalhadoAtual,
        InstanceLevel instanciaAtual,
        RecursalAuthority autoridadeAtual,
        RecursalTransitionEvent lastEvent,
        int currentRevision,
        int totalTransitions,
        int iteracoesEmbargos,
        boolean transitadoEmJulgado,
        String lastActor,
        Instant lastTransitionAt,
        RecursalSlaSnapshot sla,
        Instant createdAt,
        Instant updatedAt) {
}
