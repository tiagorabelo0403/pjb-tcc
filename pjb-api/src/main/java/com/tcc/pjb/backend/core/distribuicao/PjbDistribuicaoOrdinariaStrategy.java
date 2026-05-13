package com.tcc.pjb.backend.core.distribuicao;

import com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice.PjbUniversalDigitalCoreDecision;
import java.util.Locale;

public final class PjbDistribuicaoOrdinariaStrategy implements PjbDistribuicaoStrategy {

    @Override
    public String strategyCode() {
        return "DISTRIBUICAO_ORDINARIA_CONTEXTUAL";
    }

    @Override
    public boolean supports(PjbUniversalDigitalCoreDecision decision) {
        return decision == null || !decision.routeToDigitalCore();
    }

    @Override
    public String targetLane(PjbUniversalDigitalCoreDecision decision) {
        return decision == null ? "TRIAGEM_MANUAL" : "VARA_COMUM_ORIGEM>" + decision.targetHierarchy();
    }

    @Override
    public String targetLaneFromContext(PjbDistribuicaoContext context) {
        if (context == null) return "VARA_COMUM_ORIGEM>-";
        String rito = context.ritoCanônico() != null ? context.ritoCanônico().name() : "RITO_NULO";
        String grau = context.grau() != null ? context.grau().name() : "G1";
        String comarca = label(context.comarcaCode());
        String tribunal = label(context.tribunalCode());
        return "VARA_COMUM_ORIGEM>" + rito + ">" + grau + ">" + comarca + ">" + tribunal;
    }

    private static String label(String value) {
        return value == null || value.isBlank() ? "-" : value.strip().toUpperCase(Locale.ROOT);
    }
}
