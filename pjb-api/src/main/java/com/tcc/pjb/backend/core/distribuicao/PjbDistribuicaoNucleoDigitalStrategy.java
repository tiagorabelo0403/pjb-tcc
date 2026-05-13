package com.tcc.pjb.backend.core.distribuicao;

import com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice.PjbUniversalDigitalCoreDecision;
import java.util.Locale;

public final class PjbDistribuicaoNucleoDigitalStrategy implements PjbDistribuicaoStrategy {

    @Override
    public String strategyCode() {
        return "DISTRIBUICAO_NUCLEO_DIGITAL_UNIVERSAL";
    }

    @Override
    public boolean supports(PjbUniversalDigitalCoreDecision decision) {
        return decision != null && decision.routeToDigitalCore();
    }

    @Override
    public String targetLane(PjbUniversalDigitalCoreDecision decision) {
        return decision == null ? "TRIAGEM_MANUAL" : decision.targetHierarchy();
    }

    @Override
    public String targetLaneFromContext(PjbDistribuicaoContext context) {
        if (context == null) return "NUCLEO_DIGITAL_UNIVERSAL>-";
        String rito = context.ritoCanônico() != null ? context.ritoCanônico().name() : "RITO_NULO";
        String unidade = label(context.unidadeCode());
        String comarca = label(context.comarcaCode());
        String tribunal = label(context.tribunalCode());
        return "NUCLEO_DIGITAL_UNIVERSAL>" + rito + ">" + unidade + ">" + comarca + ">" + tribunal;
    }

    private static String label(String value) {
        return value == null || value.isBlank() ? "-" : value.strip().toUpperCase(Locale.ROOT);
    }
}
