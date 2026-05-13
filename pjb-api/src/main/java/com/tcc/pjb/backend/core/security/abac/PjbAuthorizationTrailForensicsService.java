package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailForensicsResponse;
import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailRetentionResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PjbAuthorizationTrailForensicsService {

    private final PjbAuthorizationTrailReadModelService readModelService;
    private final PjbAuthorizationTrailForensicsProjectionAssembler projectionAssembler;
    private final PjbAuthorizationTrailCsvExporter csvExporter;

    public PjbAuthorizationTrailForensicsService(PjbAuthorizationTrailReadModelService readModelService) {
        this.readModelService = readModelService;
        this.projectionAssembler = new PjbAuthorizationTrailForensicsProjectionAssembler();
        this.csvExporter = new PjbAuthorizationTrailCsvExporter();
    }

    public PjbAuthorizationTrailForensicsResponse forensics(PjbAuthorizationTrailTemporalGranularity granularity,
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
        int effectiveLimit = Math.min(10000, Math.max(1, limit == null ? 2000 : limit));
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
                effectiveLimit
        );
        List<PjbAuthorizationTrailSnapshot> snapshots = readModelService.searchForForensics(criteria, effectiveLimit);
        return projectionAssembler.assemble(granularity, effectiveLimit, snapshots);
    }

    public String exportCsv(String actionPrefix,
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
        int effectiveLimit = Math.min(10000, Math.max(1, limit == null ? 5000 : limit));
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
                effectiveLimit
        );
        return csvExporter.export(readModelService.searchForForensics(criteria, effectiveLimit));
    }

    public PjbAuthorizationTrailRetentionResponse retention(Long retentionDays) {
        long effectiveRetentionDays = Math.min(3650L, Math.max(7L, retentionDays == null ? 365L : retentionDays));
        Instant cutoff = Instant.now().minus(effectiveRetentionDays, ChronoUnit.DAYS);
        return new PjbAuthorizationTrailRetentionResponse(
                readModelService.totalEntries(),
                readModelService.oldestOccurredAt(),
                readModelService.newestOccurredAt(),
                cutoff,
                effectiveRetentionDays,
                readModelService.totalEntriesBefore(cutoff),
                5000
        );
    }
}
