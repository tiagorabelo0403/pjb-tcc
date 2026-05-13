package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.competencia.application.ProcessoCompetenciaMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.competencia.domain.ProcessoCompetenciaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.application.ProcessoDistribuicaoMalhaApplicationService;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRotaTaticaAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRotaTaticaItem;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelRotaTaticaApplicationService {

    private final ProcessoDistribuicaoMalhaApplicationService processoDistribuicaoMalhaApplicationService;
    private final ProcessoCompetenciaMalhaApplicationService processoCompetenciaMalhaApplicationService;
    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoMalhaParallelExecutor processoMalhaParallelExecutor;

    public ProcessoPainelRotaTaticaApplicationService(ProcessoDistribuicaoMalhaApplicationService processoDistribuicaoMalhaApplicationService,
                                                      ProcessoCompetenciaMalhaApplicationService processoCompetenciaMalhaApplicationService,
                                                      ProcessoRuntimeResolver processoRuntimeResolver,
                                                      ProcessoMalhaParallelExecutor processoMalhaParallelExecutor) {
        this.processoDistribuicaoMalhaApplicationService = Objects.requireNonNull(processoDistribuicaoMalhaApplicationService);
        this.processoCompetenciaMalhaApplicationService = Objects.requireNonNull(processoCompetenciaMalhaApplicationService);
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoMalhaParallelExecutor = Objects.requireNonNull(processoMalhaParallelExecutor);
    }

    public ProcessoPainelRotaTaticaAggregate detalhar(Long processoId) {
        ProcessoRuntimeContext contexto = processoRuntimeResolver.resolver(processoId);
        ProcessoMalhaParallelExecutor.Dupla<ProcessoDistribuicaoMalhaAggregate, ProcessoCompetenciaMalhaAggregate> consolidado = processoMalhaParallelExecutor.executar2(
                "painel-rota-tatica",
                () -> processoDistribuicaoMalhaApplicationService.detalhar(processoId),
                () -> processoCompetenciaMalhaApplicationService.analisar(processoId)
        );
        ProcessoDistribuicaoMalhaAggregate distribuicao = consolidado.primeiro();
        ProcessoCompetenciaMalhaAggregate competencia = consolidado.segundo();
        ArrayList<ProcessoPainelRotaTaticaItem> itens = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        itens.add(new ProcessoPainelRotaTaticaItem(
                "ROTA_DISTRIBUICAO",
                distribuicao.travaDistribuicao() ? "ALTA" : "ATENCAO",
                distribuicao.motivos().isEmpty() ? "A malha consolidou uma ação primária de distribuição." : distribuicao.motivos().getFirst().resumo(),
                distribuicao.acaoPrimaria(),
                "/api/v1/processual/distribuicao/" + processoId + "/malha"
        ));
        fundamentos.addAll(distribuicao.fundamentos());
        if (!competencia.itens().isEmpty()) {
            itens.add(new ProcessoPainelRotaTaticaItem(
                    "ROTA_COMPETENCIA",
                    (competencia.travaAtosIncompativeis() || competencia.exigeRedistribuicao()) ? "ALTA" : "ATENCAO",
                    competencia.itens().getFirst().fundamentos().isEmpty() ? "Competência consolidada pela malha nacional." : competencia.itens().getFirst().fundamentos().getFirst(),
                    competencia.acaoPrimaria(),
                    "/api/v1/processual/competencia/" + processoId + "/malha"
            ));
            fundamentos.addAll(competencia.fundamentos());
        }
        fundamentos.add("ramo=" + (contexto.ramoDireito() == null ? "NAO_INFORMADO" : contexto.ramoDireito().name()));
        return new ProcessoPainelRotaTaticaAggregate(
                processoId,
                contexto.numeroReferencia(),
                contexto.ramoDireito() == null ? "NAO_INFORMADO" : contexto.ramoDireito().name(),
                List.copyOf(itens),
                List.copyOf(fundamentos),
                Instant.now()
        );
    }
}
