package com.tcc.pjb.backend.core.processo.unificado.application;

import com.tcc.pjb.backend.core.governance.fonte.application.ProcessoFonteSoberanaApplicationService;
import com.tcc.pjb.backend.core.governance.fonte.domain.ProcessoFonteSoberanaAggregate;
import com.tcc.pjb.backend.core.processo.cooperacao.application.ProcessoCooperacaoInstitucionalApplicationService;
import com.tcc.pjb.backend.core.processo.cooperacao.domain.ProcessoCooperacaoInstitucionalAggregate;
import com.tcc.pjb.backend.core.processo.cumprimento.application.ProcessoCumprimentoOperacionalApplicationService;
import com.tcc.pjb.backend.core.processo.cumprimento.domain.ProcessoCumprimentoOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.gemeo.application.ProcessoGemeoDigitalApplicationService;
import com.tcc.pjb.backend.core.processo.gemeo.domain.ProcessoGemeoDigitalAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoInfraestruturaSoberanaAggregate;
import com.tcc.pjb.backend.core.quality.certificacao.application.PjbCertificacaoOperacionalApplicationService;
import com.tcc.pjb.backend.core.quality.certificacao.domain.PjbCertificacaoOperacionalAggregate;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoInfraestruturaSoberanaApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;
    private final ProcessoFonteSoberanaApplicationService processoFonteSoberanaApplicationService;
    private final ProcessoCumprimentoOperacionalApplicationService processoCumprimentoOperacionalApplicationService;
    private final ProcessoCooperacaoInstitucionalApplicationService processoCooperacaoInstitucionalApplicationService;
    private final PjbCertificacaoOperacionalApplicationService pjbCertificacaoOperacionalApplicationService;
    private final ProcessoGemeoDigitalApplicationService processoGemeoDigitalApplicationService;

    public ProcessoInfraestruturaSoberanaApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                            ProcessoMalhaParallelExecutor processoMalhaParallelExecutor,
                                                            ProcessoFonteSoberanaApplicationService processoFonteSoberanaApplicationService,
                                                            ProcessoCumprimentoOperacionalApplicationService processoCumprimentoOperacionalApplicationService,
                                                            ProcessoCooperacaoInstitucionalApplicationService processoCooperacaoInstitucionalApplicationService,
                                                            PjbCertificacaoOperacionalApplicationService pjbCertificacaoOperacionalApplicationService,
                                                            ProcessoGemeoDigitalApplicationService processoGemeoDigitalApplicationService) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
        this.processoFonteSoberanaApplicationService = Objects.requireNonNull(processoFonteSoberanaApplicationService);
        this.processoCumprimentoOperacionalApplicationService = Objects.requireNonNull(processoCumprimentoOperacionalApplicationService);
        this.processoCooperacaoInstitucionalApplicationService = Objects.requireNonNull(processoCooperacaoInstitucionalApplicationService);
        this.pjbCertificacaoOperacionalApplicationService = Objects.requireNonNull(pjbCertificacaoOperacionalApplicationService);
        this.processoGemeoDigitalApplicationService = Objects.requireNonNull(processoGemeoDigitalApplicationService);
    }

    @Transactional
    public ProcessoInfraestruturaSoberanaAggregate consolidar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoMalhaParallelExecutor.Quarteto<ProcessoFonteSoberanaAggregate, ProcessoCumprimentoOperacionalAggregate, ProcessoCooperacaoInstitucionalAggregate, PjbCertificacaoOperacionalAggregate> primeiraOnda = processoMalhaParallelExecutor.executar4(
                "infraestrutura-soberana-primeira-onda",
                (java.util.function.Supplier<ProcessoFonteSoberanaAggregate>) () -> processoFonteSoberanaApplicationService.consolidar(contexto),
                (java.util.function.Supplier<ProcessoCumprimentoOperacionalAggregate>) () -> processoCumprimentoOperacionalApplicationService.materializar(processoId),
                (java.util.function.Supplier<ProcessoCooperacaoInstitucionalAggregate>) () -> processoCooperacaoInstitucionalApplicationService.orquestrar(processoId),
                (java.util.function.Supplier<PjbCertificacaoOperacionalAggregate>) () -> pjbCertificacaoOperacionalApplicationService.certificar(processoId)
        );
        ProcessoGemeoDigitalAggregate gemeo = processoGemeoDigitalApplicationService.simular(processoId);
        ProcessoFonteSoberanaAggregate fonte = primeiraOnda.primeiro();
        ProcessoCumprimentoOperacionalAggregate cumprimento = primeiraOnda.segundo();
        ProcessoCooperacaoInstitucionalAggregate cooperacao = primeiraOnda.terceiro();
        PjbCertificacaoOperacionalAggregate certificacao = primeiraOnda.quarto();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("fonte.confiabilidadeMedia=" + fonte.confiabilidadeMedia());
        fundamentos.add("fonte.exigeRefresh=" + fonte.exigeRefresh());
        fundamentos.add("cumprimento.totalMaterializado=" + cumprimento.totalMaterializado());
        fundamentos.add("cooperacao.exigeRetornoExterno=" + cooperacao.exigeRetornoExterno());
        fundamentos.add("certificacao.cobertura=" + certificacao.percentualCobertura());
        fundamentos.add("gemeo.estado=" + gemeo.estadoAtual().name());
        fundamentos.addAll(cumprimento.fundamentos());
        fundamentos.addAll(cooperacao.fundamentos());
        fundamentos.addAll(certificacao.modulosCriticos());
        fundamentos.add(gemeo.gargaloProvavel());
        return new ProcessoInfraestruturaSoberanaAggregate(
                processoId,
                contexto.numeroReferencia(),
                fonte,
                cumprimento,
                cooperacao,
                certificacao,
                gemeo,
                List.copyOf(fundamentos.stream().limit(200).toList()),
                Instant.now()
        );
    }
}
