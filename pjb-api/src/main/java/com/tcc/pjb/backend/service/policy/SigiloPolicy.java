package com.tcc.pjb.backend.service.policy;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

public interface SigiloPolicy {

    NivelSigilo definirNivel(Processo processo);
}
