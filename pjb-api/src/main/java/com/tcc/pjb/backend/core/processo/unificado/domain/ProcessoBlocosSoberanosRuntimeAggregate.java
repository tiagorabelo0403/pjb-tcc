package com.tcc.pjb.backend.core.processo.unificado.domain;

import com.tcc.pjb.backend.core.governance.fonte.domain.ProcessoFonteSoberanaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.cooperacao.domain.ProcessoCooperacaoInstitucionalAggregate;
import com.tcc.pjb.backend.core.processo.cumprimento.domain.ProcessoCumprimentoOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.gemeo.domain.ProcessoGemeoDigitalAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoBlocosSoberanosRuntimeAggregate(
        Long processoId,
        String numeroProcesso,
        ProcessoFonteSoberanaAggregate fonte,
        ProcessoCumprimentoOperacionalAggregate cumprimento,
        ProcessoCooperacaoInstitucionalAggregate cooperacao,
        PjbCertificacaoOperacionalAggregate certificacao,
        ProcessoGemeoDigitalAggregate gemeo,
        int scoreGeral,
        PjbFechamentoStatus statusGeral,
        boolean prontoRuntimeSoberano,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoBlocosSoberanosRuntimeAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        scoreGeral = Math.max(0, Math.min(100, scoreGeral));
        statusGeral = statusGeral == null ? PjbFechamentoStatus.PENDENTE : statusGeral;
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
