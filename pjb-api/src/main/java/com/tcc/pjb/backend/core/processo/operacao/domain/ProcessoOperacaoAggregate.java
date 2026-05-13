package com.tcc.pjb.backend.core.processo.operacao.domain;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.Objects;

public record ProcessoOperacaoAggregate(
        ProcessoOperacaoIdentity identity,
        String readiness,
        String resilienceState,
        String observabilityState,
        String migrationState,
        double saturacaoMaxima,
        long totalBloqueios,
        List<ProcessoOperacaoFaixa> faixas,
        List<String> acoesImediatas,
        List<String> alertas,
        Instant geradoEm
) {
    public ProcessoOperacaoAggregate {
        Objects.requireNonNull(identity);
        readiness = readiness == null ? "NOT_READY" : readiness;
        resilienceState = resilienceState == null ? "ATTENTION" : resilienceState;
        observabilityState = observabilityState == null ? "ATTENTION" : observabilityState;
        migrationState = migrationState == null ? "NAO_AVALIADA" : migrationState;
        faixas = PayloadMaps.copyListDistinct(faixas);
        acoesImediatas = PayloadMaps.copyDistinctStrings(acoesImediatas);
        alertas = PayloadMaps.copyDistinctStrings(alertas);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
