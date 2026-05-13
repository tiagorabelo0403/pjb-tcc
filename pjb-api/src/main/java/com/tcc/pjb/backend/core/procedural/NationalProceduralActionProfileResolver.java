package com.tcc.pjb.backend.core.procedural;

import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralActionProfileResolver {

    private final NationalProceduralActionProfilePublicLawResolver publicLawResolver;
    private final NationalProceduralActionProfilePrivateRightsResolver privateRightsResolver;

    public NationalProceduralActionProfileResolver(NationalProceduralActionProfilePublicLawResolver publicLawResolver,
                                                   NationalProceduralActionProfilePrivateRightsResolver privateRightsResolver) {
        this.publicLawResolver = Objects.requireNonNull(publicLawResolver);
        this.privateRightsResolver = Objects.requireNonNull(privateRightsResolver);
    }

    NationalProceduralActionProfile resolve(Map<String, Object> payload,
                                            ProceduralCanonicalResolver.CanonicalContext canonical,
                                            String corpus,
                                            NationalProceduralPartyProfile partyProfile) {
        NationalProceduralActionProfileContext context = new NationalProceduralActionProfileContext(payload, canonical, corpus, partyProfile);
        return publicLawResolver.resolve(context).orElseGet(() -> privateRightsResolver.resolve(context));
    }

    String inferTrabalhistaDefaultRito(Map<String, Object> payload) {
        return publicLawResolver.inferTrabalhistaDefaultRito(payload);
    }

    String inferTrabalhistaDefaultRito(Map<String, Object> payload,
                                       String corpus,
                                       NationalProceduralPartyProfile partyProfile) {
        return publicLawResolver.inferTrabalhistaDefaultRito(payload, corpus, partyProfile);
    }

    String inferPrevidenciarioDefaultRito(Map<String, Object> payload) {
        return publicLawResolver.inferPrevidenciarioDefaultRito(payload);
    }
}
