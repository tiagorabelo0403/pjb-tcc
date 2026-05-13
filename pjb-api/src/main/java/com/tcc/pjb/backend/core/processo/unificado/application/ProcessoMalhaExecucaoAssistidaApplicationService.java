package com.tcc.pjb.backend.core.processo.unificado.application;

import com.tcc.pjb.backend.core.processo.painel.application.ProcessoPainelRotaTaticaApplicationService;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRotaTaticaAggregate;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimePreparationApplicationService;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaExecucaoAssistidaAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoMalhaNacionalFechamentoAggregate;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoMalhaExecucaoAssistidaApplicationService {

    private final ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService;
    private final ProcessoMalhaNacionalFechamentoApplicationService processoMalhaNacionalFechamentoApplicationService;
    private final ProcessoPainelRotaTaticaApplicationService processoPainelRotaTaticaApplicationService;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;

    public ProcessoMalhaExecucaoAssistidaApplicationService(ProcessoRuntimePreparationApplicationService processoRuntimePreparationApplicationService,
                                                            ProcessoMalhaNacionalFechamentoApplicationService processoMalhaNacionalFechamentoApplicationService,
                                                            ProcessoPainelRotaTaticaApplicationService processoPainelRotaTaticaApplicationService,
                                                            ProcessoMalhaParallelExecutor processoMalhaParallelExecutor) {
        this.processoRuntimePreparationApplicationService = Objects.requireNonNull(processoRuntimePreparationApplicationService);
        this.processoMalhaNacionalFechamentoApplicationService = Objects.requireNonNull(processoMalhaNacionalFechamentoApplicationService);
        this.processoPainelRotaTaticaApplicationService = Objects.requireNonNull(processoPainelRotaTaticaApplicationService);
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
    }

    @Transactional
    public ProcessoMalhaExecucaoAssistidaAggregate executar(Long processoId) {
        ProcessoMalhaParallelExecutor.Trio<ProcessoRuntimePreparationAggregate, ProcessoMalhaNacionalFechamentoAggregate, ProcessoPainelRotaTaticaAggregate> consolidado = processoMalhaParallelExecutor.executar3(
                "malha-execucao-assistida",
                () -> processoRuntimePreparationApplicationService.avaliar(processoId),
                () -> processoMalhaNacionalFechamentoApplicationService.executar(processoId),
                () -> processoPainelRotaTaticaApplicationService.detalhar(processoId)
        );
        ProcessoRuntimePreparationAggregate runtime = consolidado.primeiro();
        ProcessoMalhaNacionalFechamentoAggregate fechamento = consolidado.segundo();
        ProcessoPainelRotaTaticaAggregate rotaTatica = consolidado.terceiro();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.addAll(runtime.alertas());
        fundamentos.addAll(fechamento.fundamentos());
        fundamentos.addAll(rotaTatica.fundamentos());
        String acaoRecomendada = rotaTatica.itens().isEmpty() ? fechamento.distribuicao().acaoExecutada() : rotaTatica.itens().getFirst().acao();
        String statusExecucao = status(runtime, fechamento);
        fundamentos.add("execucao.status=" + statusExecucao);
        fundamentos.add("execucao.acao=" + acaoRecomendada);
        return new ProcessoMalhaExecucaoAssistidaAggregate(
                processoId,
                fechamento.numeroProcesso(),
                statusExecucao,
                acaoRecomendada,
                runtime,
                fechamento,
                rotaTatica,
                List.copyOf(fundamentos.stream().limit(160).toList()),
                Instant.now()
        );
    }

    private String status(ProcessoRuntimePreparationAggregate runtime,
                          ProcessoMalhaNacionalFechamentoAggregate fechamento) {
        if (!runtime.prontoParaMalhaCompleta()) {
            return "ASSISTIDA_POR_RUNTIME";
        }
        if (fechamento.distribuicao().bloqueada()) {
            return "BLOQUEADA_POR_MALHA";
        }
        if (fechamento.antifraude().scoreGlobal() >= 85) {
            return "ESCALADA_ANTIFRAUDE";
        }
        if (fechamento.distribuicao().remessaManual() || fechamento.distribuicao().redistribuicaoManual()) {
            return "TRIAGEM_OPERACIONAL";
        }
        return "PRONTA_PARA_ORQUESTRACAO";
    }
}
