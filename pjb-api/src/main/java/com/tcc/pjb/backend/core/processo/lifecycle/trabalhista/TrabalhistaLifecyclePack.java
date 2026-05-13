package com.tcc.pjb.backend.core.processo.lifecycle.trabalhista;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class TrabalhistaLifecyclePack extends AbstractRitoLifecyclePack {

    public TrabalhistaLifecyclePack() {
        super(RitoGrupoPrincipal.TRABALHISTA);
    }

    @Override
    public FaseProcessual executionPhase(RitoProcessual rito) {
        return rito == RitoProcessual.TRABALHISTA_EXECUCAO ? FaseProcessual.EXECUCAO : FaseProcessual.CUMPRIMENTO_SENTENCA;
    }

    @Override
    public FaseProcessual reopenPhase(RitoProcessual rito) {
        return executionPhase(rito);
    }

    @Override
    public String magistratureDesk(RitoProcessual rito) {
        return "MAGISTRATURA_TRABALHISTA";
    }

    @Override
    public String collegiateDesk(RitoProcessual rito) {
        return "COLEGIADO_TRT_TST";
    }
}
