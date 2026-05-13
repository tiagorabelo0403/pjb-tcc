package com.tcc.pjb.backend.core.procedural;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

final class NationalProceduralActionProfileSupport {

    private NationalProceduralActionProfileSupport() {
    }

    static NationalProceduralActionProfile profile(String actionNature,
                                                   String actionFamily,
                                                   boolean specialProcedure,
                                                   String defaultRito,
                                                   String varaFamily,
                                                   Collection<String> markers,
                                                   Collection<String> reasons,
                                                   Collection<String> legalBases,
                                                   Collection<String> alerts,
                                                   Collection<String> reviewChecklist,
                                                   String reason,
                                                   String legalBase) {
        LinkedHashSet<String> normalizedMarkers = orderedSet(markers);
        LinkedHashSet<String> normalizedReasons = orderedSet(reasons);
        LinkedHashSet<String> normalizedLegalBases = orderedSet(legalBases);
        LinkedHashSet<String> normalizedAlerts = orderedSet(alerts);
        LinkedHashSet<String> normalizedReviewChecklist = orderedSet(reviewChecklist);
        if (!isBlank(actionNature)) {
            normalizedMarkers.add(actionNature.trim());
        }
        if (!isBlank(actionFamily)) {
            normalizedMarkers.add(actionFamily.trim());
        }
        if (!isBlank(reason)) {
            normalizedReasons.add(reason.trim());
        }
        if (!isBlank(legalBase)) {
            normalizedLegalBases.add(legalBase.trim());
        }
        return new NationalProceduralActionProfile(
                actionNature,
                actionFamily,
                specialProcedure,
                defaultRito,
                varaFamily,
                List.copyOf(normalizedMarkers),
                List.copyOf(normalizedReasons),
                List.copyOf(normalizedLegalBases),
                List.copyOf(normalizedAlerts),
                List.copyOf(normalizedReviewChecklist)
        );
    }

    static NationalProceduralActionProfile rebuilt(NationalProceduralActionProfile profile,
                                                   Collection<String> alerts,
                                                   Collection<String> reviewChecklist) {
        return new NationalProceduralActionProfile(
                profile.actionNature(),
                profile.actionFamily(),
                profile.specialProcedure(),
                profile.defaultRito(),
                profile.varaFamily(),
                List.copyOf(orderedSet(profile.markers())),
                List.copyOf(orderedSet(profile.reasons())),
                List.copyOf(orderedSet(profile.legalBases())),
                List.copyOf(orderedSet(alerts)),
                List.copyOf(orderedSet(reviewChecklist))
        );
    }

    static LinkedHashSet<String> orderedSet(Collection<String> values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (values == null) {
            return set;
        }
        for (String value : values) {
            if (!isBlank(value)) {
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

    static String normalize(String value) {
        return NationalProceduralRoutingSupport.normalize(value);
    }

    static boolean isBlank(String value) {
        return NationalProceduralRoutingSupport.isBlank(value);
    }

    static String text(Object value) {
        return NationalProceduralRoutingSupport.text(value);
    }

    static BigDecimal decimal(Object value) {
        return NationalProceduralRoutingSupport.decimal(value);
    }
}
