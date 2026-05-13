package com.tcc.pjb.backend.core.plataforma.substituicao.readiness;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoCapacidadeStatus;
import java.util.Objects;

public record PjbTribunalReadinessCapability(
        String codigo,
        String titulo,
        PjbSubstituicaoCapacidadeStatus status,
        boolean homologada,
        boolean bloqueada,
        String eixoPjb,
        String proximaEntrega
) {
    public PjbTribunalReadinessCapability {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        status = status == null ? PjbSubstituicaoCapacidadeStatus.FALTANTE : status;
        eixoPjb = Objects.toString(eixoPjb, "").trim();
        proximaEntrega = Objects.toString(proximaEntrega, "").trim();
    }

    public boolean pronta() {
        return status == PjbSubstituicaoCapacidadeStatus.PRESENTE && homologada && !bloqueada;
    }

    public boolean exigeHomologacao() {
        return status != PjbSubstituicaoCapacidadeStatus.FALTANTE && !homologada;
    }
}
