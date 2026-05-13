package com.tcc.pjb.backend.core.processo.completude.domain;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.hardening.domain.ProcessoHardeningAggregate;
import com.tcc.pjb.backend.core.processo.orfandade.domain.ProcessoAntiOrfaoAggregate;
import com.tcc.pjb.backend.core.processo.plantao.domain.ProcessoPlantaoSubstituicaoAggregate;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.core.processo.sinalizacao.domain.ProcessoSinalizacaoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoInfraestruturaSoberanaAggregate;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import java.time.Instant;
import java.util.List;

public record ProcessoFechamentoTotalAggregate(
        Long processoId,
        String numeroProcesso,
        String readiness,
        long scoreGeral,
        ProcessoHardeningAggregate hardening,
        ProcessoAntiOrfaoAggregate antiOrfao,
        ProcessoSinalizacaoAggregate sinalizacao,
        ProcessoPlantaoSubstituicaoAggregate plantaoSubstituicao,
        ProcessoAnalyticsNacionalAggregate analyticsNacional,
        ProcessoOperacaoTransversalAggregate operacaoTransversal,
        ProcessoInfraestruturaSoberanaAggregate infraestruturaSoberana,
        PjbCertificacaoOperacionalAggregate certificacaoOperacional,
        PjbSubstituicaoLegadosAggregate substituicaoLegados,
        PjbCodebaseSanityAggregate codebaseSanity,
        List<String> alertas,
        List<String> plano,
        Instant geradoEm
) {
    public ProcessoFechamentoTotalAggregate {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        readiness = readiness == null || readiness.isBlank() ? "NOT_READY" : readiness;
        scoreGeral = Math.max(0L, Math.min(100L, scoreGeral));
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        plano = plano == null ? List.of() : List.copyOf(plano);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
