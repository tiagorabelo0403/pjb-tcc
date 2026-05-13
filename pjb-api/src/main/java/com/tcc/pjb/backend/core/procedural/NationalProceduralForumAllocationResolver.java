package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralForumAllocationResolver {

    private final NationalProceduralForumAllocationSeedResolver seedResolver;
    private final NationalProceduralForumRoutingReadinessResolver readinessResolver;
    private final NationalProceduralForumAllocationReportAssembler reportAssembler;

    public NationalProceduralForumAllocationResolver(NationalProceduralForumAllocationSeedResolver seedResolver,
                                                     NationalProceduralForumRoutingReadinessResolver readinessResolver,
                                                     NationalProceduralForumAllocationReportAssembler reportAssembler) {
        this.seedResolver = Objects.requireNonNull(seedResolver);
        this.readinessResolver = Objects.requireNonNull(readinessResolver);
        this.reportAssembler = Objects.requireNonNull(reportAssembler);
    }

    ProceduralForumAllocationReport resolve(NationalProceduralForumAllocationContext context) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(context.canonical());
        Objects.requireNonNull(context.competence());
        Objects.requireNonNull(context.tipoJustica());
        Objects.requireNonNull(context.actionProfile());
        Objects.requireNonNull(context.juizadoDecision());
        NationalProceduralForumAllocationSeed seed = seedResolver.resolve(context);
        NationalProceduralForumRoutingReadiness readiness = readinessResolver.resolve(context, seed);
        return reportAssembler.assemble(context, seed, readiness);
    }
}
