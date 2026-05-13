package com.tcc.pjb.backend.core.processo.conexao.application;

import com.tcc.pjb.backend.core.processo.conexao.domain.ProcessoConexaoAggregate;
import com.tcc.pjb.backend.core.processo.conexao.domain.ProcessoConexaoItem;
import com.tcc.pjb.backend.core.processo.prevencao.application.ProcessoPrevencaoConexaoDependenciaEngine;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseConsulta;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculoTipo;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoConexaoApplicationService {

    private final ProcessoPrevencaoConexaoDependenciaEngine engine;

    public ProcessoConexaoApplicationService(ProcessoPrevencaoConexaoDependenciaEngine engine) {
        this.engine = Objects.requireNonNull(engine);
    }

    public ProcessoConexaoAggregate analisar(ProcessoVinculacaoAnaliseConsulta consulta) {
        var aggregate = engine.analisar(consulta);
        List<ProcessoConexaoItem> itens = aggregate.itens().stream()
                .filter(item -> item.tipo() == ProcessoVinculoTipo.CONEXAO)
                .map(item -> new ProcessoConexaoItem(
                        item.processoId(),
                        item.numeroProcesso(),
                        item.natureza(),
                        item.score(),
                        item.chavesCompartilhadas(),
                        item.fundamentos()
                ))
                .sorted(Comparator.comparingDouble(ProcessoConexaoItem::score).reversed().thenComparing(ProcessoConexaoItem::numeroProcesso))
                .toList();
        return new ProcessoConexaoAggregate(
                aggregate.processoIdRaiz(),
                aggregate.numeroProcessoRaiz(),
                !itens.isEmpty(),
                itens.size(),
                itens,
                aggregate.fundamentos(),
                Instant.now()
        );
    }
}
