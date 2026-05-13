package com.tcc.pjb.backend.core.distribuicao;

import com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice.PjbUniversalDigitalCoreDecision;
import java.util.Locale;

public interface PjbDistribuicaoStrategy {

    String strategyCode();

    boolean supports(PjbUniversalDigitalCoreDecision decision);

    String targetLane(PjbUniversalDigitalCoreDecision decision);

    default String targetLaneFromContext(PjbDistribuicaoContext context) {
        if (context == null) return strategyCode() + ">-";
        return strategyCode()
                + ">" + label(context.ritoCanônico() != null ? context.ritoCanônico().name() : "RITO_NULO")
                + ">" + label(context.grau() != null ? context.grau().name() : "G1")
                + ">" + label(context.comarcaCode())
                + ">" + label(context.tribunalCode());
    }

    private static String label(String value) {
        return value == null || value.isBlank() ? "-" : value.strip().toUpperCase(Locale.ROOT);
    }
}
