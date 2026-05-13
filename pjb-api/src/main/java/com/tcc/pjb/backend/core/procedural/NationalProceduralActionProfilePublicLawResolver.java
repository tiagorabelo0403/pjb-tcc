package com.tcc.pjb.backend.core.procedural;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralActionProfilePublicLawResolver {

    private final NationalProceduralActionProfileSpecialProcedureResolver specialProcedureResolver;
    private final NationalProceduralActionProfileLaborCriminalResolver laborCriminalResolver;
    private final NationalProceduralActionProfilePublicEntityResolver publicEntityResolver;
    private final NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver;

    public NationalProceduralActionProfilePublicLawResolver(NationalProceduralActionProfileSpecialProcedureResolver specialProcedureResolver,
                                                            NationalProceduralActionProfileLaborCriminalResolver laborCriminalResolver,
                                                            NationalProceduralActionProfilePublicEntityResolver publicEntityResolver,
                                                            NationalProceduralActionProfileEconomicRitoResolver economicRitoResolver) {
        this.specialProcedureResolver = Objects.requireNonNull(specialProcedureResolver);
        this.laborCriminalResolver = Objects.requireNonNull(laborCriminalResolver);
        this.publicEntityResolver = Objects.requireNonNull(publicEntityResolver);
        this.economicRitoResolver = Objects.requireNonNull(economicRitoResolver);
    }

    Optional<NationalProceduralActionProfile> resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        return specialProcedureResolver.resolve(context)
                .or(() -> laborCriminalResolver.resolve(context))
                .or(() -> publicEntityResolver.resolve(context));
    }

    String inferTrabalhistaDefaultRito(Map<String, Object> payload) {
        return economicRitoResolver.inferTrabalhistaDefaultRito(payload);
    }

    String inferTrabalhistaDefaultRito(Map<String, Object> payload,
                                       String corpus,
                                       NationalProceduralPartyProfile partyProfile) {
        return economicRitoResolver.inferTrabalhistaDefaultRito(payload, corpus, partyProfile);
    }

    String inferPrevidenciarioDefaultRito(Map<String, Object> payload) {
        return economicRitoResolver.inferPrevidenciarioDefaultRito(payload);
    }
}
