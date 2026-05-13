package com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh;

import java.util.LinkedHashSet;
import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;

public record RecursalMeshDashboardRequest(
        @Size(max = 240) String q,
        @Positive Long processoId,
        List<Long> processoIds,
        @Size(max = 30) String speciesCode,
        RecursalLifecycleState currentState,
        RecursalTribunal tribunalAtual,
        RecursalTribunalDetalhado tribunalDetalhadoAtual,
        RecursalAuthority autoridadeAtual,
        @Size(max = 120) String precedenteCodigo,
        @Size(max = 120) String precedenteTribunal,
        @Size(max = 240) String precedenteTema,
        Boolean sobrestadoPrecedente,
        Boolean precedenteAplicado,
        Boolean precedenteDistinguido,
        Boolean transitadoEmJulgado,
        Boolean slaVencido,
        Boolean slaFatalParaPartes,
        @Min(50) @Max(2000) Integer scanLimit,
        @Min(1) @Max(20) Integer bucketLimit) {

    public RecursalMeshDashboardRequest {
        q = q == null ? null : q.trim();
        speciesCode = speciesCode == null ? null : speciesCode.trim();
        precedenteCodigo = precedenteCodigo == null ? null : precedenteCodigo.trim();
        precedenteTribunal = precedenteTribunal == null ? null : precedenteTribunal.trim();
        precedenteTema = precedenteTema == null ? null : precedenteTema.trim();
        processoIds = normalizeProcessoIds(processoIds);
        scanLimit = scanLimit == null ? 500 : Math.max(50, Math.min(2000, scanLimit));
        bucketLimit = bucketLimit == null ? 10 : Math.max(1, Math.min(20, bucketLimit));
    }

    public RecursalMeshDashboardRequest withProcessoIds(List<Long> processoIds) {
        return new RecursalMeshDashboardRequest(
                q,
                processoId,
                processoIds,
                speciesCode,
                currentState,
                tribunalAtual,
                tribunalDetalhadoAtual,
                autoridadeAtual,
                precedenteCodigo,
                precedenteTribunal,
                precedenteTema,
                sobrestadoPrecedente,
                precedenteAplicado,
                precedenteDistinguido,
                transitadoEmJulgado,
                slaVencido,
                slaFatalParaPartes,
                scanLimit,
                bucketLimit
        );
    }

    private static List<Long> normalizeProcessoIds(List<Long> processoIds) {
        if (processoIds == null || processoIds.isEmpty()) {
            return List.of();
        }
        return processoIds.stream()
                .filter(java.util.Objects::nonNull)
                .filter(id -> id > 0)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        ids -> ids.stream().limit(500).toList()
                ));
    }
}
