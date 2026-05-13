package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailQueryResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PjbAuthorizationTrailAdminService {

    private final PjbAuthorizationTrailRegistry registry;
    private final PjbAuthorizationTrailReadModelService readModelService;
    private final PjbAuthorizationTrailProjectionAssembler projectionAssembler;

    public PjbAuthorizationTrailAdminService(PjbAuthorizationTrailRegistry registry,
                                             PjbAuthorizationTrailReadModelService readModelService) {
        this.registry = registry;
        this.readModelService = readModelService;
        this.projectionAssembler = new PjbAuthorizationTrailProjectionAssembler();
    }

    public PjbAuthorizationTrailQueryResponse search(PjbAuthorizationTrailSourceMode sourceMode,
                                                     String actionPrefix,
                                                     String resourceType,
                                                     String resourceId,
                                                     Long actorId,
                                                     String actorType,
                                                     Boolean allowed,
                                                     PjbAuthorizationRiskLevel riskLevel,
                                                     String requestId,
                                                     String integrationCode,
                                                     String institutionalUnitCode,
                                                     String institutionalBoxCode,
                                                     String institutionalCapabilityCode,
                                                     String governanceChannel,
                                                     String governanceScope,
                                                     Boolean governanceRequired,
                                                     Boolean governanceSatisfied,
                                                     Boolean stepUpRequired,
                                                     Boolean stepUpSatisfied,
                                                     Instant occurredAfter,
                                                     Instant occurredBefore,
                                                     Integer limit) {
        PjbAuthorizationTrailSourceMode effectiveMode = sourceMode == null ? PjbAuthorizationTrailSourceMode.PERSISTED : sourceMode;
        PjbAuthorizationTrailQueryCriteria criteria = new PjbAuthorizationTrailQueryCriteria(
                actionPrefix,
                resourceType,
                resourceId,
                actorId,
                actorType,
                allowed,
                riskLevel,
                requestId,
                integrationCode,
                institutionalUnitCode,
                institutionalBoxCode,
                institutionalCapabilityCode,
                governanceChannel,
                governanceScope,
                governanceRequired,
                governanceSatisfied,
                stepUpRequired,
                stepUpSatisfied,
                occurredAfter,
                occurredBefore,
                limit == null ? 100 : limit
        );
        List<PjbAuthorizationTrailSnapshot> persisted = effectiveMode == PjbAuthorizationTrailSourceMode.RUNTIME
                ? List.of()
                : readModelService.search(criteria);
        List<PjbAuthorizationTrailSnapshot> runtime = effectiveMode == PjbAuthorizationTrailSourceMode.PERSISTED
                ? List.of()
                : registry.search(criteria);
        List<PjbAuthorizationTrailSnapshot> selected = switch (effectiveMode) {
            case RUNTIME -> runtime;
            case PERSISTED -> persisted;
            case MERGED -> projectionAssembler.merge(persisted, runtime, criteria.limit());
        };
        return projectionAssembler.assemble(
                effectiveMode,
                readModelService.totalEntries(),
                registry.totalEntries(),
                selected
        );
    }
}
