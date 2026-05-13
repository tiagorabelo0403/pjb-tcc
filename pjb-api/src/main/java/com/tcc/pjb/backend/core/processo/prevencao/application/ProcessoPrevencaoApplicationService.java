package com.tcc.pjb.backend.core.processo.prevencao.application;

import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoPrevencaoAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoPrevencaoItem;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseAggregate;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseConsulta;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPrevencaoApplicationService {

    private final ProcessoPrevencaoConexaoDependenciaEngine engine;

    public ProcessoPrevencaoApplicationService(ProcessoPrevencaoConexaoDependenciaEngine engine) {
        this.engine = Objects.requireNonNull(engine);
    }

    public ProcessoPrevencaoAggregate analisar(ProcessoVinculacaoAnaliseConsulta consulta) {
        ProcessoVinculacaoAnaliseAggregate aggregate = engine.analisar(consulta);
        List<ProcessoPrevencaoItem> itens = engine.extrairPrevencao(aggregate).stream()
                .sorted(Comparator.comparingDouble(ProcessoPrevencaoItem::score).reversed()
                        .thenComparing(ProcessoPrevencaoItem::numeroProcesso))
                .toList();
        ProcessoPrevencaoItem prevento = itens.stream()
                .min(Comparator.comparing(ProcessoPrevencaoItem::distribuidoEm).thenComparing(ProcessoPrevencaoItem::numeroProcesso))
                .orElse(null);
        return new ProcessoPrevencaoAggregate(
                aggregate.processoIdRaiz(),
                aggregate.numeroProcessoRaiz(),
                !itens.isEmpty(),
                prevento == null ? null : prevento.numeroProcesso(),
                prevento == null ? null : prevento.unidadeSugerida(),
                itens,
                aggregate.fundamentos(),
                Instant.now()
        );
    }
}
