package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewInputRequirementResolver {

    private final NationalProceduralReviewCoreFieldRequirementResolver coreFieldRequirementResolver;
    private final NationalProceduralReviewEconomicRequirementResolver economicRequirementResolver;
    private final NationalProceduralReviewLocationRequirementResolver locationRequirementResolver;
    private final NationalProceduralReviewPartyRequirementResolver partyRequirementResolver;
    private final NationalProceduralReviewJurisdictionRequirementResolver jurisdictionRequirementResolver;

    @Inject
    public NationalProceduralReviewInputRequirementResolver(NationalProceduralReviewCoreFieldRequirementResolver coreFieldRequirementResolver,
                                                            NationalProceduralReviewEconomicRequirementResolver economicRequirementResolver,
                                                            NationalProceduralReviewLocationRequirementResolver locationRequirementResolver,
                                                            NationalProceduralReviewPartyRequirementResolver partyRequirementResolver,
                                                            NationalProceduralReviewJurisdictionRequirementResolver jurisdictionRequirementResolver) {
        this.coreFieldRequirementResolver = Objects.requireNonNull(coreFieldRequirementResolver);
        this.economicRequirementResolver = Objects.requireNonNull(economicRequirementResolver);
        this.locationRequirementResolver = Objects.requireNonNull(locationRequirementResolver);
        this.partyRequirementResolver = Objects.requireNonNull(partyRequirementResolver);
        this.jurisdictionRequirementResolver = Objects.requireNonNull(jurisdictionRequirementResolver);
    }



    NationalProceduralReviewInputRequirementResolver(NationalProceduralReviewCoreFieldRequirementResolver coreFieldRequirementResolver,
                                                     NationalProceduralReviewEconomicRequirementResolver economicRequirementResolver,
                                                     NationalProceduralReviewLocationRequirementResolver locationRequirementResolver,
                                                     NationalProceduralReviewPartyRequirementResolver partyRequirementResolver) {
        this(coreFieldRequirementResolver, economicRequirementResolver, locationRequirementResolver, partyRequirementResolver,
                new NationalProceduralReviewJurisdictionRequirementResolver(new NationalProceduralReviewMessages()));
    }

    NationalProceduralReviewInputAssessment assess(NationalProceduralReviewSynthesisContext context) {
        Objects.requireNonNull(context);
        NationalProceduralReviewInputSlice coreFields = coreFieldRequirementResolver.assess(context.payload());
        NationalProceduralReviewInputSlice economic = economicRequirementResolver.assess(context.payload(), context.juizadoDecision(), context.actionProfile());
        NationalProceduralReviewInputSlice location = locationRequirementResolver.assess(context.cidadeSugerida(), context.ufSugerida());
        NationalProceduralReviewInputSlice party = partyRequirementResolver.assess(context.payload());
        NationalProceduralReviewInputSlice jurisdiction = jurisdictionRequirementResolver.assess(context);

        LinkedHashSet<String> missingInputs = orderedSet(coreFields.missingInputs());
        addAll(missingInputs, economic.missingInputs());
        addAll(missingInputs, location.missingInputs());
        addAll(missingInputs, party.missingInputs());
        addAll(missingInputs, jurisdiction.missingInputs());

        LinkedHashSet<String> blockingIssues = orderedSet(coreFields.blockingIssues());
        addAll(blockingIssues, economic.blockingIssues());
        addAll(blockingIssues, location.blockingIssues());
        addAll(blockingIssues, party.blockingIssues());
        addAll(blockingIssues, jurisdiction.blockingIssues());

        return new NationalProceduralReviewInputAssessment(
                List.copyOf(missingInputs),
                List.copyOf(blockingIssues)
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
