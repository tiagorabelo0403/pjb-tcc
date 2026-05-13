package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public final class PjbSettlementGovernanceLens {

    public PjbSettlementOpportunityReport evaluate(PjbSettlementOpportunitySignal signal) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> safeguards = new LinkedHashSet<>();
        int score = 0;
        if (signal != null && signal.repeatLitigant()) {
            score += 20;
            reasons.add("litigante repetitivo permite política de composição controlada");
        }
        if (signal != null && signal.documentaryEvidence()) {
            score += 25;
            reasons.add("prova documental reduz incerteza para proposta assistida");
        }
        if (signal != null && signal.stableJurisprudence()) {
            score += 25;
            reasons.add("jurisprudência estável favorece estimativa de risco");
        }
        if (signal != null && signal.claimValue() != null && signal.claimValue().compareTo(BigDecimal.valueOf(100000)) <= 0) {
            score += 15;
            reasons.add("valor permite avaliação proporcional de composição");
        }
        if (signal != null && signal.vulnerableParty()) {
            safeguards.add("exigir linguagem simples e revisão humana reforçada");
        }
        if (signal != null && signal.publicPolicyRestriction()) {
            safeguards.add("bloquear sugestão automática incompatível com indisponibilidade do interesse público");
            score = Math.min(score, 35);
        }
        if (safeguards.isEmpty()) {
            safeguards.add("registrar proposta como sugestão assistida sem coerção decisória");
        }
        String status = signal != null && signal.publicPolicyRestriction() ? "RESTRICTED_BY_PUBLIC_POLICY" : score >= 70 ? "STRONG_SETTLEMENT_WINDOW" : score >= 45 ? "MODERATE_SETTLEMENT_WINDOW" : "LOW_SETTLEMENT_FIT";
        return new PjbSettlementOpportunityReport(status, Math.max(0, Math.min(100, score)), true, new ArrayList<>(reasons), new ArrayList<>(safeguards));
    }
}
