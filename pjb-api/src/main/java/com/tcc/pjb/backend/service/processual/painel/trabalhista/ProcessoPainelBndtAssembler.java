package com.tcc.pjb.backend.service.processual.painel.trabalhista;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelBndtAggregate;
import com.tcc.pjb.backend.model.dto.processual.painel.trabalhista.ProcessoPainelBndtResponse;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelBndtAssembler {

    public ProcessoPainelBndtResponse toResponse(ProcessoPainelBndtAggregate aggregate) {
        return new ProcessoPainelBndtResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.aplicavel(),
                aggregate.status(),
                aggregate.consultaTempoReal(),
                aggregate.fonteOficial(),
                aggregate.fallbackMode(),
                aggregate.alertas(),
                aggregate.proximosPassos(),
                aggregate.geradoEm()
        );
    }
}
