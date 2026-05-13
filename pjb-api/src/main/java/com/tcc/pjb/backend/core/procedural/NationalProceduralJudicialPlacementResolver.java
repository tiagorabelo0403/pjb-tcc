package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJudicialPlacementResolver {

    private final NationalProceduralJudicialPlacementSeedResolver seedResolver;
    private final NationalProceduralJudicialPlacementFinalizer finalizer;

    public NationalProceduralJudicialPlacementResolver(NationalProceduralJudicialPlacementSeedResolver seedResolver,
                                                       NationalProceduralJudicialPlacementFinalizer finalizer) {
        this.seedResolver = Objects.requireNonNull(seedResolver);
        this.finalizer = Objects.requireNonNull(finalizer);
    }

    NationalProceduralJudicialPlacement resolve(NationalProceduralJudicialPlacementContext context) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(context.canonical());
        Objects.requireNonNull(context.competence());
        Objects.requireNonNull(context.tipoJustica());
        Objects.requireNonNull(context.actionProfile());
        Objects.requireNonNull(context.juizadoDecision());
        NationalProceduralJudicialPlacementSeed seed = seedResolver.resolve(context);
        return finalizer.finalizePlacement(context, seed);
    }
}
