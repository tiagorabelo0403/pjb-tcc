package com.tcc.pjb.backend.service.processual.painel.rota;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRotaTaticaAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelRotaTaticaItem;
import com.tcc.pjb.backend.model.dto.processual.painel.rota.ProcessoPainelRotaTaticaItemResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.rota.ProcessoPainelRotaTaticaResponse;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelRotaTaticaAssembler {

    public ProcessoPainelRotaTaticaResponse toResponse(ProcessoPainelRotaTaticaAggregate aggregate) {
        return new ProcessoPainelRotaTaticaResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.ramoDireito(),
                aggregate.itens().stream().map(this::toItem).toList(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    private ProcessoPainelRotaTaticaItemResponse toItem(ProcessoPainelRotaTaticaItem item) {
        return new ProcessoPainelRotaTaticaItemResponse(
                item.code(),
                item.severity(),
                item.fundamento(),
                item.acao(),
                item.navigationPath()
        );
    }
}
