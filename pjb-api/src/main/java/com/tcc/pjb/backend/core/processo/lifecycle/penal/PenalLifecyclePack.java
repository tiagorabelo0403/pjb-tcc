package com.tcc.pjb.backend.core.processo.lifecycle.penal;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class PenalLifecyclePack extends AbstractRitoLifecyclePack {

    public PenalLifecyclePack() {
        super(RitoGrupoPrincipal.PENAL);
    }

    @Override
    public FaseProcessual initialPhase(RitoProcessual rito, FaseProcessual fallback) {
        return rito == RitoProcessual.TRIBUNAL_JURI ? FaseProcessual.PRONUNCIA : FaseProcessual.CONHECIMENTO;
    }

    @Override
    public FaseProcessual decisionPhase(RitoProcessual rito) {
        return rito == RitoProcessual.TRIBUNAL_JURI ? FaseProcessual.PRONUNCIA : FaseProcessual.CONHECIMENTO;
    }

    @Override
    public FaseProcessual executionPhase(RitoProcessual rito) {
        return FaseProcessual.EXECUCAO;
    }

    @Override
    public FaseProcessual reopenPhase(RitoProcessual rito) {
        return decisionPhase(rito);
    }

    @Override
    public String magistratureDesk(RitoProcessual rito) {
        return "MAGISTRATURA_PENAL";
    }

    @Override
    public String collegiateDesk(RitoProcessual rito) {
        return "COLEGIADO_CRIMINAL";
    }
}
