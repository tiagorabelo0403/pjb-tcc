package com.tcc.pjb.backend.service.policy.impl;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.service.policy.SigiloPolicy;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SigiloPolicyTJRJ implements SigiloPolicy {

    private final SigiloPolicyPadrao padrao;

    @Override
    public NivelSigilo definirNivel(Processo processo) {
        
        
        return padrao.definirNivel(processo);
    }
}
