package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralActionProfilePrivateRightsResolver {

    private final NationalProceduralActionProfileInfanciaResolver infanciaResolver;
    private final NationalProceduralActionProfileFamilyResolver familyResolver;
    private final NationalProceduralActionProfilePropertyResolver propertyResolver;
    private final NationalProceduralActionProfileBusinessResolver businessResolver;
    private final NationalProceduralActionProfileConsumerResolver consumerResolver;

    public NationalProceduralActionProfilePrivateRightsResolver(NationalProceduralActionProfileInfanciaResolver infanciaResolver,
                                                               NationalProceduralActionProfileFamilyResolver familyResolver,
                                                               NationalProceduralActionProfilePropertyResolver propertyResolver,
                                                               NationalProceduralActionProfileBusinessResolver businessResolver,
                                                               NationalProceduralActionProfileConsumerResolver consumerResolver) {
        this.infanciaResolver = Objects.requireNonNull(infanciaResolver);
        this.familyResolver = Objects.requireNonNull(familyResolver);
        this.propertyResolver = Objects.requireNonNull(propertyResolver);
        this.businessResolver = Objects.requireNonNull(businessResolver);
        this.consumerResolver = Objects.requireNonNull(consumerResolver);
    }

    NationalProceduralActionProfile resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        return infanciaResolver.resolve(context)
                .or(() -> familyResolver.resolve(context))
                .or(() -> propertyResolver.resolve(context))
                .or(() -> businessResolver.resolve(context))
                .orElseGet(() -> consumerResolver.resolve(context));
    }
}
