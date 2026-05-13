package com.tcc.pjb.backend.core.processo.lifecycle.previdenciario;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class PrevidenciarioLifecyclePack extends AbstractRitoLifecyclePack {

    public PrevidenciarioLifecyclePack() {
        super(RitoGrupoPrincipal.PREVIDENCIARIO);
    }

    @Override
    public String collegiateDesk(RitoProcessual rito) {
        return rito != null && rito.isJuizado() ? "TURMA_RECURSAL" : "COLEGIADO_PREVIDENCIARIO";
    }
}
