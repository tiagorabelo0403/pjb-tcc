package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoCapacidadeNacional;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoCapacidadeStatus;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalCapabilityCatalog;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoSistemaLegado;
import java.util.List;

public final class PjbLegacyReplacementQueryService {

    public List<PjbSubstituicaoCapacidadeNacional> capabilitiesFor(PjbSubstituicaoSistemaLegado sistema) {
        return PjbSubstituicaoNacionalCapabilityCatalog.porSistema(sistema);
    }

    public List<PjbSubstituicaoCapacidadeNacional> gapsFor(PjbSubstituicaoSistemaLegado sistema) {
        return capabilitiesFor(sistema).stream()
                .filter(PjbSubstituicaoCapacidadeNacional::pendente)
                .toList();
    }

    public boolean productionEquivalent(PjbSubstituicaoSistemaLegado sistema) {
        List<PjbSubstituicaoCapacidadeNacional> capabilities = capabilitiesFor(sistema);
        return !capabilities.isEmpty() && capabilities.stream().allMatch(capability -> capability.status() == PjbSubstituicaoCapacidadeStatus.PRESENTE);
    }
}
