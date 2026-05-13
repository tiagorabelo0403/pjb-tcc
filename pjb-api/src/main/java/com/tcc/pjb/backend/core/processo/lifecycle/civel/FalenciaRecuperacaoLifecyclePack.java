package com.tcc.pjb.backend.core.processo.lifecycle.civel;

import com.tcc.pjb.backend.core.processo.lifecycle.AbstractRitoLifecyclePack;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;

@Component
public final class FalenciaRecuperacaoLifecyclePack extends AbstractRitoLifecyclePack {

    public FalenciaRecuperacaoLifecyclePack() {
        super(RitoGrupoPrincipal.FALENCIA_RECUPERACAO);
    }
}
