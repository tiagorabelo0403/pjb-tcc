package com.tcc.pjb.backend.core.processo.lifecycle.penal;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class ExecucaoPenalLifecyclePack extends AbstractRitoLifecyclePack {

    public ExecucaoPenalLifecyclePack() {
        super(RitoGrupoPrincipal.EXECUCAO_PENAL);
    }

    @Override
    public FaseProcessual initialPhase(RitoProcessual rito, FaseProcessual fallback) {
        return FaseProcessual.EXECUCAO;
    }

    @Override
    public FaseProcessual executionPhase(RitoProcessual rito) {
        return FaseProcessual.EXECUCAO;
    }

    @Override
    public FaseProcessual reopenPhase(RitoProcessual rito) {
        return FaseProcessual.EXECUCAO;
    }
}
