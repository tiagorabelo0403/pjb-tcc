package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewSignalCollector {

    private final NationalProceduralReviewReasonCollector reviewReasonCollector;
    private final NationalProceduralReviewPolicySignalResolver reviewPolicySignalResolver;

    public NationalProceduralReviewSignalCollector(NationalProceduralReviewReasonCollector reviewReasonCollector,
                                                   NationalProceduralReviewPolicySignalResolver reviewPolicySignalResolver) {
        this.reviewReasonCollector = Objects.requireNonNull(reviewReasonCollector);
        this.reviewPolicySignalResolver = Objects.requireNonNull(reviewPolicySignalResolver);
    }

    NationalProceduralReviewDraft collect(NationalProceduralReviewSynthesisContext context) {
        Objects.requireNonNull(context);
        NationalProceduralReviewDraft reasonDraft = reviewReasonCollector.collect(context);
        NationalProceduralReviewSignalSet policySignals = reviewPolicySignalResolver.collect(context);
        LinkedHashSet<String> alerts = orderedSet(policySignals.alerts());
        LinkedHashSet<String> reviewChecklist = orderedSet(policySignals.reviewChecklist());
        LinkedHashSet<String> blockingIssues = orderedSet(policySignals.blockingIssues());
        return new NationalProceduralReviewDraft(
                reasonDraft.reasons(),
                reasonDraft.legalBases(),
                List.copyOf(alerts),
                List.copyOf(reviewChecklist),
                List.copyOf(blockingIssues),
                reasonDraft.actionMarkers()
        );
    }

    private static LinkedHashSet<String> orderedSet(List<String> values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        addAll(set, values);
        return set;
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
