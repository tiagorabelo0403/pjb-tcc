package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record DiligenceOperationalCommandCenterResponse(
        String canal,
        String operatorPerfil,
        String unidadeBase,
        Summary summary,
        List<ProcessBucket> processBuckets,
        List<UnitBucket> unitBuckets,
        List<OrganizationBucket> organizationBuckets,
        List<String> alerts
) {

    public record Summary(
            long annexations,
            long queuedDispatches,
            long dispatched,
            long acknowledged,
            long backlog,
            long processes,
            long units,
            long organizations,
            Instant lastMovementAt
    ) {
    }

    public record ProcessBucket(
            Long processoId,
            String processoNumero,
            String unidadeLabel,
            String organizationLabel,
            long annexations,
            long dispatches,
            long acknowledged,
            long backlog,
            Instant lastAt,
            List<String> stages
    ) {
    }

    public record UnitBucket(
            String unidadeLabel,
            long processos,
            long dispatches,
            long acknowledged,
            long backlog,
            Instant lastAt
    ) {
    }

    public record OrganizationBucket(
            String organizationLabel,
            long processos,
            long unidades,
            long dispatches,
            long acknowledged,
            long backlog,
            Instant lastAt
    ) {
    }
}
