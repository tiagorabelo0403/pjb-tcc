package com.tcc.pjb.backend.core.processo.lifecycle.tributario;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class ExecucaoFiscalLifecyclePack extends AbstractRitoLifecyclePack {

    public ExecucaoFiscalLifecyclePack() {
        super(RitoGrupoPrincipal.EXECUCAO_FISCAL);
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

    @Override
    public String magistratureDesk(RitoProcessual rito) {
        return "MAGISTRATURA_FAZENDA";
    }

    @Override
    public String collegiateDesk(RitoProcessual rito) {
        return "COLEGIADO_FAZENDA";
    }
}
