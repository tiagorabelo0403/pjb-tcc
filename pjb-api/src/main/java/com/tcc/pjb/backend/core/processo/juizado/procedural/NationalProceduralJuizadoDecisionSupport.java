package com.tcc.pjb.backend.core.processo.juizado.procedural;

import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingSupport;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

final class NationalProceduralJuizadoDecisionSupport {

    private NationalProceduralJuizadoDecisionSupport() {
    }

    static NationalProceduralJuizadoDecision decision(boolean admiteJuizado,
                                                      String ritoOverride,
                                                      Collection<String> reasons,
                                                      Collection<String> legalBases,
                                                      Collection<String> alerts,
                                                      Collection<String> reviewChecklist,
                                                      double confidence,
                                                      boolean requiresReview) {
        return new NationalProceduralJuizadoDecision(
                admiteJuizado,
                ritoOverride,
                List.copyOf(orderedSet(reasons)),
                List.copyOf(orderedSet(legalBases)),
                List.copyOf(orderedSet(alerts)),
                List.copyOf(orderedSet(reviewChecklist)),
                confidence,
                requiresReview
        );
    }

    static LinkedHashSet<String> orderedSet(Collection<String> values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (values == null) {
            return set;
        }
        for (String value : values) {
            if (!NationalProceduralRoutingSupport.isBlank(value)) {
                set.add(value.trim());
            }
        }
        return set;
    }

    static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    static boolean containsAny(String value, String... keys) {
        return NationalProceduralRoutingSupport.containsAny(value, keys);
    }

    static BigDecimal decimal(Object value) {
        return NationalProceduralRoutingSupport.decimal(value);
    }
}
