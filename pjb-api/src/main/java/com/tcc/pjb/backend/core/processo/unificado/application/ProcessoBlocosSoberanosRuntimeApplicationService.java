package com.tcc.pjb.backend.core.processo.unificado.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.governance.fonte.domain.ProcessoFonteSoberanaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.cooperacao.domain.ProcessoCooperacaoInstitucionalAggregate;
import com.tcc.pjb.backend.core.processo.cumprimento.domain.ProcessoCumprimentoOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.gemeo.domain.ProcessoGemeoDigitalAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoBlocosSoberanosRuntimeAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoInfraestruturaSoberanaAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoBlocosSoberanosRuntimeApplicationService {

    private final ProcessoInfraestruturaSoberanaApplicationService processoInfraestruturaSoberanaApplicationService;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;

    public ProcessoBlocosSoberanosRuntimeApplicationService(ProcessoInfraestruturaSoberanaApplicationService processoInfraestruturaSoberanaApplicationService,
                                                            ProcessoMalhaSupportBridge processoMalhaSupportBridge) {
        this.processoInfraestruturaSoberanaApplicationService = Objects.requireNonNull(processoInfraestruturaSoberanaApplicationService);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
    }

    @Transactional(readOnly = true)
    public ProcessoBlocosSoberanosRuntimeAggregate avaliar(Long processoId) {
        ProcessoInfraestruturaSoberanaAggregate infraestrutura = processoInfraestruturaSoberanaApplicationService.consolidar(processoId);
        ProcessoFonteSoberanaAggregate fonte = infraestrutura.fonte();
        ProcessoCumprimentoOperacionalAggregate cumprimento = infraestrutura.cumprimento();
        ProcessoCooperacaoInstitucionalAggregate cooperacao = infraestrutura.cooperacao();
        PjbCertificacaoOperacionalAggregate certificacao = infraestrutura.certificacao();
        ProcessoGemeoDigitalAggregate gemeo = infraestrutura.gemeo();
        int scoreFonte = fonte.confiabilidadeMedia();
        int scoreCumprimento = cumprimento.totalMaterializado() > 0 ? 82 : 58;
        int scoreCooperacao = cooperacao.exigeRetornoExterno() ? 68 : 84;
        int scoreCertificacao = certificacao.percentualCobertura();
        int scoreGemeo = Math.max(10, 100 - gemeo.custoOperacionalEstimado());
        int scoreGeral = Math.max(0, Math.min(100, (scoreFonte + scoreCumprimento + scoreCooperacao + scoreCertificacao + scoreGemeo) / 5));
        boolean pronto = scoreGeral >= 80 && !certificacao.existeFalhaCritica();
        PjbFechamentoStatus statusGeral = pronto ? PjbFechamentoStatus.CONCLUIDA : scoreGeral >= 60 ? PjbFechamentoStatus.PARCIAL : PjbFechamentoStatus.BLOQUEADA;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(infraestrutura.fundamentos());
        fundamentos.add("scoreFonte=" + scoreFonte);
        fundamentos.add("scoreCumprimento=" + scoreCumprimento);
        fundamentos.add("scoreCooperacao=" + scoreCooperacao);
        fundamentos.add("scoreCertificacao=" + scoreCertificacao);
        fundamentos.add("scoreGemeo=" + scoreGemeo);
        DecisionTraceService trace = processoMalhaSupportBridge.decisionTraceService();
        if (trace != null) {
            trace.record("plataforma.blocos.soberanos.runtime", "Processo", String.valueOf(processoId), BigDecimal.valueOf(scoreGeral), infraestrutura.toString(), fundamentos.toString(), Hashes.sha256Hex(infraestrutura.numeroProcesso()), Hashes.sha256Hex(fundamentos.toString()), "PJB-SOBERANO", statusGeral.name());
        }
        AuditLedgerService audit = processoMalhaSupportBridge.auditLedgerService();
        if (audit != null) {
            audit.appendSafely("PJB_BLOCOS_SOBERANOS_AVALIADOS", "Processo", String.valueOf(processoId), Hashes.sha256Hex(fundamentos.toString()), "status=" + statusGeral.name() + ";score=" + scoreGeral);
        }
        return new ProcessoBlocosSoberanosRuntimeAggregate(
                processoId,
                infraestrutura.numeroProcesso(),
                fonte,
                cumprimento,
                cooperacao,
                certificacao,
                gemeo,
                scoreGeral,
                statusGeral,
                pronto,
                List.copyOf(fundamentos.stream().limit(140).toList()),
                Instant.now()
        );
    }
}
