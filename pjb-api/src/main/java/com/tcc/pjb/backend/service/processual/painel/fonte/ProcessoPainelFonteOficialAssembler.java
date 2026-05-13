package com.tcc.pjb.backend.service.processual.painel.fonte;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialItem;
import com.tcc.pjb.backend.model.dto.processual.painel.fonte.ProcessoPainelFonteOficialItemResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.fonte.ProcessoPainelFonteOficialResponse;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelFonteOficialAssembler {

    public ProcessoPainelFonteOficialResponse toResponse(ProcessoPainelFonteOficialAggregate aggregate) {
        return new ProcessoPainelFonteOficialResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.ramoDireito(),
                aggregate.itens().stream().map(this::toItem).toList(),
                aggregate.garantias(),
                aggregate.geradoEm()
        );
    }

    private ProcessoPainelFonteOficialItemResponse toItem(ProcessoPainelFonteOficialItem item) {
        return new ProcessoPainelFonteOficialItemResponse(
                item.widgetCode(),
                item.dominio(),
                item.officialSources(),
                item.fallbackMode(),
                item.idempotencyMode(),
                item.replayMode(),
                item.forensicMode()
        );
    }
}
