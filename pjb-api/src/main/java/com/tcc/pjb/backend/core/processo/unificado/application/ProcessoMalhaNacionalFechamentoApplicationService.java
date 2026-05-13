package com.tcc.pjb.backend.core.processo.unificado.application;

import com.tcc.pjb.backend.core.processo.anomalia.application.ProcessoAntifraudeOperacionalApplicationService;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAntifraudeOperacionalAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.application.ProcessoDistribuicaoMalhaOrquestracaoApplicationService;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaOrquestracaoAggregate;
import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelMalhaPapelApplicationService;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelMalhaPapelAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimePreparationApplicationService;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalFechamentoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaObservabilidadeAggregate;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoMalhaNacionalFechamentoApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService;
    private final ProcessoDistribuicaoMalhaOrquestracaoApplicationService processoDistribuicaoMalhaOrquestracaoApplicationService;
    private final ProcessoMalhaObservabilidadeApplicationService processoMalhaObservabilidadeApplicationService;
    private final ProcessoAntifraudeOperacionalApplicationService processoAntifraudeOperacionalApplicationService;
    private final ProcessoPainelMalhaPapelApplicationService processoPainelMalhaPapelApplicationService;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;

    public ProcessoMalhaNacionalFechamentoApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                             ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService,
                                                             ProcessoDistribuicaoMalhaOrquestracaoApplicationService processoDistribuicaoMalhaOrquestracaoApplicationService,
                                                             ProcessoMalhaObservabilidadeApplicationService processoMalhaObservabilidadeApplicationService,
                                                             ProcessoAntifraudeOperacionalApplicationService processoAntifraudeOperacionalApplicationService,
                                                             ProcessoPainelMalhaPapelApplicationService processoPainelMalhaPapelApplicationService,
                                                             ProcessoMalhaParallelExecutor processoMalhaParallelExecutor) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoRuntimePreparationApplicationService = Objects.requireNonNull(processoRuntimePreparationApplicationService);
        this.processoDistribuicaoMalhaOrquestracaoApplicationService = Objects.requireNonNull(processoDistribuicaoMalhaOrquestracaoApplicationService);
        this.processoMalhaObservabilidadeApplicationService = Objects.requireNonNull(processoMalhaObservabilidadeApplicationService);
        this.processoAntifraudeOperacionalApplicationService = Objects.requireNonNull(processoAntifraudeOperacionalApplicationService);
        this.processoPainelMalhaPapelApplicationService = Objects.requireNonNull(processoPainelMalhaPapelApplicationService);
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
    }

    @Transactional
    public ProcessoMalhaNacionalFechamentoAggregate executar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoMalhaParallelExecutor.Quarteto<ProcessoRuntimePreparationAggregate, ProcessoDistribuicaoMalhaOrquestracaoAggregate, ProcessoMalhaObservabilidadeAggregate, ProcessoAntifraudeOperacionalAggregate> consolidado = processoMalhaParallelExecutor.executar4(
                "fechamento-final-malha",
                (java.util.function.Supplier<ProcessoRuntimePreparationAggregate>) () -> processoRuntimePreparationApplicationService.avaliar(contexto),
                (java.util.function.Supplier<ProcessoDistribuicaoMalhaOrquestracaoAggregate>) () -> processoDistribuicaoMalhaOrquestracaoApplicationService.executar(processoId),
                (java.util.function.Supplier<ProcessoMalhaObservabilidadeAggregate>) () -> processoMalhaObservabilidadeApplicationService.detalhar(processoId),
                (java.util.function.Supplier<ProcessoAntifraudeOperacionalAggregate>) () -> processoAntifraudeOperacionalApplicationService.acionar(processoId)
        );
        ProcessoRuntimePreparationAggregate runtime = consolidado.primeiro();
        ProcessoDistribuicaoMalhaOrquestracaoAggregate distribuicao = consolidado.segundo();
        ProcessoMalhaObservabilidadeAggregate observabilidade = consolidado.terceiro();
        ProcessoAntifraudeOperacionalAggregate antifraude = consolidado.quarto();
        ProcessoPainelMalhaPapelAggregate painelPapel = processoPainelMalhaPapelApplicationService.detalhar(processoId, contexto.papelPrincipal(), contexto.ramoDireito());
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(runtime.alertas());
        fundamentos.addAll(distribuicao.fundamentos());
        fundamentos.addAll(observabilidade.fundamentos());
        fundamentos.addAll(antifraude.fundamentos());
        fundamentos.addAll(painelPapel.fundamentos());
        fundamentos.add("runtime.pronto=" + runtime.prontoParaMalhaCompleta());
        fundamentos.add("runtime.prontidao=" + runtime.integrationStatus().percentualProntidao());
        return new ProcessoMalhaNacionalFechamentoAggregate(
                processoId,
                contexto.numeroReferencia(),
                runtime,
                distribuicao,
                observabilidade,
                antifraude,
                painelPapel,
                List.copyOf(fundamentos.stream().limit(120).toList()),
                Instant.now()
        );
    }
}
