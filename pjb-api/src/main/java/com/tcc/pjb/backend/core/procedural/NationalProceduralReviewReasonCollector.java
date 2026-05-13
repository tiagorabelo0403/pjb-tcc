package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewReasonCollector {

    NationalProceduralReviewDraft collect(NationalProceduralReviewSynthesisContext context) {
        Objects.requireNonNull(context);
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> legalBases = new LinkedHashSet<>();
        LinkedHashSet<String> actionMarkers = new LinkedHashSet<>(PayloadMaps.copyDistinctStrings(context.actionProfile().markers()));
        addAll(reasons, context.competence().reasons());
        addAll(reasons, context.actionProfile().reasons());
        addAll(reasons, context.juizadoDecision().reasons());
        addAll(legalBases, context.competence().legalBases());
        addAll(legalBases, context.actionProfile().legalBases());
        addAll(legalBases, context.juizadoDecision().legalBases());
        if (context.teto().bloqueante() && !NationalProceduralRoutingSupport.isBlank(context.teto().fundamentoLegal())) {
            legalBases.add(context.teto().fundamentoLegal().trim());
        }
        return new NationalProceduralReviewDraft(
                List.copyOf(reasons),
                List.copyOf(legalBases),
                List.of(),
                List.of(),
                List.of(),
                List.copyOf(actionMarkers)
        );
    }

    private static void addAll(Set<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (!NationalProceduralRoutingSupport.isBlank(value)) {
                target.add(value.trim());
            }
        }
    }
}
