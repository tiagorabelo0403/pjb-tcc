package com.tcc.pjb.backend.core.processo.lifecycle.civel;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class JuizadoLifecyclePack extends AbstractRitoLifecyclePack {

    public JuizadoLifecyclePack() {
        super(RitoGrupoPrincipal.JUIZADO);
    }

    @Override
    public String collegiateDesk(RitoProcessual rito) {
        return "TURMA_RECURSAL";
    }
}
