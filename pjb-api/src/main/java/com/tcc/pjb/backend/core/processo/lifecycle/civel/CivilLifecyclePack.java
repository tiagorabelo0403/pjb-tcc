package com.tcc.pjb.backend.core.processo.lifecycle.civel;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class CivilLifecyclePack extends AbstractRitoLifecyclePack {

    public CivilLifecyclePack() {
        super(RitoGrupoPrincipal.CIVIL);
    }

    @Override
    public boolean supports(RitoProcessual rito) {
        return rito == null || super.supports(rito);
    }

    @Override
    public FaseProcessual initialPhase(RitoProcessual rito, FaseProcessual fallback) {
        return fallback == null ? FaseProcessual.CONHECIMENTO : fallback;
    }
}
