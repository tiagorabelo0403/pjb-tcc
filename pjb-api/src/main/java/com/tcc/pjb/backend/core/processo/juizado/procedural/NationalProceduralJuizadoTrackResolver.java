package com.tcc.pjb.backend.core.processo.juizado.procedural;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJuizadoTrackResolver {

    private final NationalProceduralJuizadoTrackClassifier classifier;
    private final NationalProceduralJuizadoFederalTrackResolver federalTrackResolver;
    private final NationalProceduralJuizadoFazendaTrackResolver fazendaTrackResolver;
    private final NationalProceduralJuizadoCivelTrackResolver civelTrackResolver;
    private final NationalProceduralJuizadoCriminalTrackResolver criminalTrackResolver;

    public NationalProceduralJuizadoTrackResolver(NationalProceduralJuizadoTrackClassifier classifier,
                                                  NationalProceduralJuizadoFederalTrackResolver federalTrackResolver,
                                                  NationalProceduralJuizadoFazendaTrackResolver fazendaTrackResolver,
                                                  NationalProceduralJuizadoCivelTrackResolver civelTrackResolver,
                                                  NationalProceduralJuizadoCriminalTrackResolver criminalTrackResolver) {
        this.classifier = Objects.requireNonNull(classifier);
        this.federalTrackResolver = Objects.requireNonNull(federalTrackResolver);
        this.fazendaTrackResolver = Objects.requireNonNull(fazendaTrackResolver);
        this.civelTrackResolver = Objects.requireNonNull(civelTrackResolver);
        this.criminalTrackResolver = Objects.requireNonNull(criminalTrackResolver);
    }

    NationalProceduralJuizadoDecision resolve(NationalProceduralJuizadoDecisionContext context) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(context.actionProfile());
        Objects.requireNonNull(context.competence());
        Objects.requireNonNull(context.teto());
        return switch (classifier.classify(context)) {
            case FEDERAL -> federalTrackResolver.resolve(context);
            case FAZENDA -> fazendaTrackResolver.resolve(context);
            case CIVEL -> civelTrackResolver.resolve(context);
            case CRIMINAL -> criminalTrackResolver.resolve(context);
            case NONE -> NationalProceduralJuizadoDecisionSupport.decision(false, null, null, null, null, null, 0.78d, false);
        };
    }
}
