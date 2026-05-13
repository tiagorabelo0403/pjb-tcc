package com.tcc.pjb.backend.core.processo.lifecycle;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public interface RitoLifecyclePack {
    RitoGrupoPrincipal grupoPrincipal();

    boolean supports(RitoProcessual rito);

    FaseProcessual initialPhase(RitoProcessual rito, FaseProcessual fallback);

    FaseProcessual audiencePhase(RitoProcessual rito);

    FaseProcessual decisionPhase(RitoProcessual rito);

    FaseProcessual executionPhase(RitoProcessual rito);

    FaseProcessual reopenPhase(RitoProcessual rito);

    String magistratureDesk(RitoProcessual rito);

    String collegiateDesk(RitoProcessual rito);
}
