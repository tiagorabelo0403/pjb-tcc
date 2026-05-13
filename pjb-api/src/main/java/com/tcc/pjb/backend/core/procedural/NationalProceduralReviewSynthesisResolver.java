package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewSynthesisResolver {

    private final NationalProceduralReviewSignalCollector reviewSignalCollector;
    private final NationalProceduralReviewInputRequirementResolver reviewInputRequirementResolver;
    private final NationalProceduralConfidenceResolver confidenceResolver;

    public NationalProceduralReviewSynthesisResolver(NationalProceduralReviewSignalCollector reviewSignalCollector,
                                                     NationalProceduralReviewInputRequirementResolver reviewInputRequirementResolver,
                                                     NationalProceduralConfidenceResolver confidenceResolver) {
        this.reviewSignalCollector = Objects.requireNonNull(reviewSignalCollector);
        this.reviewInputRequirementResolver = Objects.requireNonNull(reviewInputRequirementResolver);
        this.confidenceResolver = Objects.requireNonNull(confidenceResolver);
    }

    NationalProceduralReviewSynthesis resolve(NationalProceduralReviewSynthesisContext context) {
        Objects.requireNonNull(context);
        NationalProceduralReviewDraft reviewDraft = reviewSignalCollector.collect(context);
        NationalProceduralReviewInputAssessment inputAssessment = reviewInputRequirementResolver.assess(context);

        LinkedHashSet<String> alerts = orderedSet(reviewDraft.alerts());
        LinkedHashSet<String> missingInputs = orderedSet(inputAssessment.missingInputs());
        LinkedHashSet<String> reviewChecklist = orderedSet(reviewDraft.reviewChecklist());
        LinkedHashSet<String> blockingIssues = orderedSet(reviewDraft.blockingIssues());
        addAll(blockingIssues, inputAssessment.blockingIssues());

        NationalProceduralConfidenceAssessment confidenceAssessment = confidenceResolver.assess(
                context.selectedRito(),
                context.competence(),
                context.juizadoDecision(),
                context.forumAllocation(),
                context.distribution(),
                missingInputs,
                alerts,
                context.teto()
        );

        return new NationalProceduralReviewSynthesis(
                reviewDraft.reasons(),
                reviewDraft.legalBases(),
                List.copyOf(alerts),
                List.copyOf(missingInputs),
                reviewDraft.actionMarkers(),
                List.copyOf(reviewChecklist),
                List.copyOf(blockingIssues),
                confidenceAssessment.confidence(),
                confidenceAssessment.requiresHumanReview(),
                confidenceAssessment.riskLevel()
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
