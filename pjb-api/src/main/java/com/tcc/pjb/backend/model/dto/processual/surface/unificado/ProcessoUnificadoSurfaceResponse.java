package com.tcc.pjb.backend.model.dto.processual.surface.unificado;

import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceIdentityResponse;

import java.time.Instant;
import java.util.List;

public record ProcessoUnificadoSurfaceResponse(
        ProcessoSurfaceIdentityResponse identity,
        ProcessoSurfaceCompetenciaResponse competencia,
        ProcessoSurfaceDiagnosticoResponse diagnostico,
        List<ProcessoSurfaceAtoResponse> atosPermitidos,
        List<ProcessoSurfaceAtoResponse> atosBloqueados,
        List<String> proximoMelhorAto,
        Instant generatedAt
) {
}
