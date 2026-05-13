package com.tcc.pjb.backend.core.processo.lifecycle.eleitoral;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class EleitoralLifecyclePack extends AbstractRitoLifecyclePack {

    public EleitoralLifecyclePack() {
        super(RitoGrupoPrincipal.ELEITORAL);
    }

    @Override
    public String magistratureDesk(RitoProcessual rito) {
        return "MAGISTRATURA_ELEITORAL";
    }

    @Override
    public String collegiateDesk(RitoProcessual rito) {
        return "COLEGIADO_TRE_TSE";
    }
}
