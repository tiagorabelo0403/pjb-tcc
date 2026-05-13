package com.tcc.pjb.backend.model.dto.processual.surface.unificado;

import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceValueItemResponse;

import java.time.Instant;
import java.util.List;

public record ProcessoSurfaceDiagnosticoResponse(
        boolean healthy,
        long totalFindings,
        long blockingFindings,
        long atosPermitidos,
        long atosBloqueados,
        long atosSensiveis,
        long atosComSegurancaElevada,
        List<ProcessoSurfaceValueItemResponse> findings,
        List<String> fundamentos,
        Instant generatedAt
) {
}
