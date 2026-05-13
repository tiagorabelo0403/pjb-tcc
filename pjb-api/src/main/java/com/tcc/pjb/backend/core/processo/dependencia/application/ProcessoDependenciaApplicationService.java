package com.tcc.pjb.backend.core.processo.dependencia.application;

import com.tcc.pjb.backend.core.processo.dependencia.domain.ProcessoDependenciaAggregate;
import com.tcc.pjb.backend.core.processo.dependencia.domain.ProcessoDependenciaItem;
import com.tcc.pjb.backend.core.processo.prevencao.application.ProcessoPrevencaoConexaoDependenciaEngine;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseConsulta;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculoTipo;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoDependenciaApplicationService {

    private final ProcessoPrevencaoConexaoDependenciaEngine engine;

    public ProcessoDependenciaApplicationService(ProcessoPrevencaoConexaoDependenciaEngine engine) {
        this.engine = Objects.requireNonNull(engine);
    }

    public ProcessoDependenciaAggregate analisar(ProcessoVinculacaoAnaliseConsulta consulta) {
        var aggregate = engine.analisar(consulta);
        List<ProcessoDependenciaItem> itens = aggregate.itens().stream()
                .filter(item -> item.tipo() == ProcessoVinculoTipo.DEPENDENCIA)
                .map(item -> new ProcessoDependenciaItem(
                        item.processoId(),
                        item.numeroProcesso(),
                        item.natureza(),
                        item.score(),
                        item.bloquearDistribuicao(),
                        item.fundamentos()
                ))
                .sorted(Comparator.comparingDouble(ProcessoDependenciaItem::score).reversed().thenComparing(ProcessoDependenciaItem::numeroProcesso))
                .toList();
        return new ProcessoDependenciaAggregate(
                aggregate.processoIdRaiz(),
                aggregate.numeroProcessoRaiz(),
                !itens.isEmpty(),
                itens,
                aggregate.fundamentos(),
                Instant.now()
        );
    }
}
