package com.tcc.pjb.backend.core.processo.lifecycle.militar;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class MilitarLifecyclePack extends AbstractRitoLifecyclePack {

    public MilitarLifecyclePack() {
        super(RitoGrupoPrincipal.MILITAR);
    }

    @Override
    public String magistratureDesk(RitoProcessual rito) {
        return "MAGISTRATURA_MILITAR";
    }

    @Override
    public String collegiateDesk(RitoProcessual rito) {
        return "COLEGIADO_MILITAR";
    }
}
