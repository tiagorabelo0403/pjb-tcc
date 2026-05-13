package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record DiligenceOperationalAnalyticsResponse(
        String canal,
        String diligenceReference,
        ProcessScope processScope,
        OperatorScope operatorScope,
        UnitScope unitScope
) {

    public record ProcessScope(
            Long processoId,
            String processoNumero,
            String currentStage,
            long telemetrias,
            long checkpoints,
            long certidoes,
            long encerramentos,
            long formalizacoes,
            long juntadas,
            long anexacoesInstitucionais,
            long documentosProcessuais,
            long eventosProcessuais,
            long tentativasRegistradas,
            Instant firstAt,
            Instant lastAt,
            Long cycleSeconds,
            Double hitRateCheckpoint,
            Double annexationRate,
            List<String> highlights
    ) {
    }

    public record OperatorScope(
            Long operatorUserId,
            String operatorPerfil,
            String unidadeLabel,
            long telemetrias,
            long checkpoints,
            long certidoes,
            long encerramentos,
            long formalizacoes,
            long juntadas,
            long anexacoesInstitucionais,
            long closedCycles,
            Double annexationCoverage,
            Instant lastOperationalAt,
            List<String> highlights
    ) {
    }

    public record UnitScope(
            String unidadeLabel,
            long telemetrias,
            long checkpoints,
            long certidoes,
            long encerramentos,
            long formalizacoes,
            long juntadas,
            long anexacoesInstitucionais,
            long operadoresAtivos,
            long processosImpactados,
            Instant lastOperationalAt,
            List<String> highlights
    ) {
    }
}
