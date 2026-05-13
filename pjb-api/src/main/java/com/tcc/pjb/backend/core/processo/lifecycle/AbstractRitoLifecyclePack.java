package com.tcc.pjb.backend.core.processo.lifecycle;

import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public abstract class AbstractRitoLifecyclePack implements RitoLifecyclePack {

    private final RitoGrupoPrincipal grupoPrincipal;

    protected AbstractRitoLifecyclePack(RitoGrupoPrincipal grupoPrincipal) {
        this.grupoPrincipal = Objects.requireNonNull(grupoPrincipal);
    }

    @Override
    public final RitoGrupoPrincipal grupoPrincipal() {
        return grupoPrincipal;
    }

    @Override
    public boolean supports(RitoProcessual rito) {
        return rito != null && rito.getGrupoPrincipal() == grupoPrincipal;
    }

    @Override
    public FaseProcessual initialPhase(RitoProcessual rito, FaseProcessual fallback) {
        return fallback;
    }

    @Override
    public FaseProcessual audiencePhase(RitoProcessual rito) {
        return FaseProcessual.INSTRUTORIA;
    }

    @Override
    public FaseProcessual decisionPhase(RitoProcessual rito) {
        return FaseProcessual.CONHECIMENTO;
    }

    @Override
    public FaseProcessual executionPhase(RitoProcessual rito) {
        return FaseProcessual.CUMPRIMENTO_SENTENCA;
    }

    @Override
    public FaseProcessual reopenPhase(RitoProcessual rito) {
        if (rito != null && (rito.name().contains("CUMPRIMENTO") || rito.name().contains("EXECUCAO"))) {
            return executionPhase(rito);
        }
        return FaseProcessual.CONHECIMENTO;
    }

    @Override
    public String magistratureDesk(RitoProcessual rito) {
        return "MAGISTRATURA";
    }

    @Override
    public String collegiateDesk(RitoProcessual rito) {
        return "COLEGIADO";
    }
}
