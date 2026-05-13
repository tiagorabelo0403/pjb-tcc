package com.tcc.pjb.backend.core.processo.unificado.domain;

import com.tcc.pjb.backend.core.governance.fonte.domain.ProcessoFonteSoberanaAggregate;
import com.tcc.pjb.backend.core.processo.cooperacao.domain.ProcessoCooperacaoInstitucionalAggregate;
import com.tcc.pjb.backend.core.processo.cumprimento.domain.ProcessoCumprimentoOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.gemeo.domain.ProcessoGemeoDigitalAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoInfraestruturaSoberanaAggregate(Long processoId,
                                                      String numeroProcesso,
                                                      ProcessoFonteSoberanaAggregate fonte,
                                                      ProcessoCumprimentoOperacionalAggregate cumprimento,
                                                      ProcessoCooperacaoInstitucionalAggregate cooperacao,
                                                      PjbCertificacaoOperacionalAggregate certificacao,
                                                      ProcessoGemeoDigitalAggregate gemeo,
                                                      List<String> fundamentos,
                                                      Instant geradoEm) {
    public ProcessoInfraestruturaSoberanaAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        fonte = Objects.requireNonNull(fonte);
        cumprimento = Objects.requireNonNull(cumprimento);
        cooperacao = Objects.requireNonNull(cooperacao);
        certificacao = Objects.requireNonNull(certificacao);
        gemeo = Objects.requireNonNull(gemeo);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
