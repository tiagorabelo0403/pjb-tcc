package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshIndexDriftReport;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryModel;
import com.tcc.pjb.backend.query.recursalmesh.RecursalMeshQueryRepository;

@Service
public class RecursalMeshIndexDriftService {

    private final RecursalProcessIntegrationStateRepository projectionRepository;
    private final ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider;
    private final ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider;

    public RecursalMeshIndexDriftService(RecursalProcessIntegrationStateRepository projectionRepository,
                                         ObjectProvider<RecursalMeshQueryRepository> queryRepositoryProvider,
                                         ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider) {
        this.projectionRepository = Objects.requireNonNull(projectionRepository);
        this.queryRepositoryProvider = Objects.requireNonNull(queryRepositoryProvider);
        this.telemetryProvider = Objects.requireNonNull(telemetryProvider);
    }

    public RecursalMeshIndexDriftReport assess(Integer sampleSize) {
        int normalizedSampleSize = normalizeSampleSize(sampleSize);
        RecursalMeshQueryRepository queryRepository = queryRepositoryProvider.getIfAvailable();
        long projectionCount = projectionRepository.count();
        if (queryRepository == null) {
            RecursalMeshIndexDriftReport report = new RecursalMeshIndexDriftReport(
                    "SEARCH_DISABLED",
                    RecursalMeshQueryModel.INDEX_NAME,
                    projectionCount,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    "UNKNOWN",
                    Instant.now()
            );
            RecursalMeshOperationalTelemetryService telemetryService = telemetryProvider.getIfAvailable();
            if (telemetryService != null) {
                telemetryService.updateDrift(report);
            }
            return report;
        }
        long indexCount = queryRepository.count();
        List<RecursalProcessIntegrationState> sample = projectionRepository.findAll(PageRequest.of(0, normalizedSampleSize, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("recursoId")))).getContent();
        int missingInIndex = 0;
        int outdatedInIndex = 0;
        int divergentState = 0;
        int divergentRevision = 0;
        for (RecursalProcessIntegrationState projection : sample) {
            if (projection == null || projection.getRecursoId() == null || projection.getRecursoId().isBlank()) {
                continue;
            }
            RecursalMeshQueryModel doc = queryRepository.findById(projection.getRecursoId()).orElse(null);
            if (doc == null) {
                missingInIndex++;
                continue;
            }
            if (projection.getUpdatedAt() != null && doc.getUpdatedAt() != null && doc.getUpdatedAt().isBefore(projection.getUpdatedAt())) {
                outdatedInIndex++;
            }
            if (!sameEnumName(projection.getCurrentState(), doc.getCurrentState())) {
                divergentState++;
            }
            if (!Objects.equals(projection.getCurrentRevision(), doc.getCurrentRevision())) {
                divergentRevision++;
            }
        }
        String severity = severity(projectionCount, indexCount, sample.size(), missingInIndex, outdatedInIndex, divergentState, divergentRevision);
        RecursalMeshIndexDriftReport report = new RecursalMeshIndexDriftReport(
                "ASSESSED",
                RecursalMeshQueryModel.INDEX_NAME,
                projectionCount,
                indexCount,
                sample.size(),
                missingInIndex,
                outdatedInIndex,
                divergentState,
                divergentRevision,
                severity,
                Instant.now()
        );
        RecursalMeshOperationalTelemetryService telemetryService = telemetryProvider.getIfAvailable();
        if (telemetryService != null) {
            telemetryService.updateDrift(report);
        }
        return report;
    }

    private static boolean sameEnumName(Enum<?> left, String right) {
        if (left == null) {
            return right == null || right.isBlank();
        }
        return left.name().equalsIgnoreCase(right == null ? null : right.trim());
    }

    private static int normalizeSampleSize(Integer sampleSize) {
        if (sampleSize == null) {
            return 100;
        }
        return Math.max(10, Math.min(500, sampleSize));
    }

    private static String severity(long projectionCount,
                                   long indexCount,
                                   int sampled,
                                   int missingInIndex,
                                   int outdatedInIndex,
                                   int divergentState,
                                   int divergentRevision) {
        if (sampled == 0 && projectionCount == 0L && indexCount == 0L) {
            return "HEALTHY";
        }
        long countGap = Math.abs(projectionCount - indexCount);
        int driftScore = missingInIndex + outdatedInIndex + divergentState + divergentRevision;
        if (countGap > 1000 || driftScore > Math.max(10, sampled / 2)) {
            return "CRITICAL";
        }
        if (countGap > 100 || driftScore > Math.max(4, sampled / 5)) {
            return "HIGH";
        }
        if (countGap > 0 || driftScore > 0) {
            return "MONITOR";
        }
        return "HEALTHY";
    }
}
