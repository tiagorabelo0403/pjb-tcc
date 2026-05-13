package com.tcc.pjb.backend.model.dto.governance;

import java.time.Instant;
import java.util.List;

public record CodebaseLearningResponse(
        boolean available,
        boolean criticalHotspotsPresent,
        int totalMainFiles,
        int totalTestFiles,
        int integrationTests,
        int mappedCoreSlices,
        List<HotspotResponse> hotspots,
        List<ExtractionBlueprintResponse> extractionBlueprints,
        List<CriticalFlowResponse> criticalFlows,
        List<String> priorityWaves,
        List<String> learnings,
        Instant generatedAt
) {
    public CodebaseLearningResponse {
        hotspots = hotspots == null ? List.of() : List.copyOf(hotspots);
        extractionBlueprints = extractionBlueprints == null ? List.of() : List.copyOf(extractionBlueprints);
        criticalFlows = criticalFlows == null ? List.of() : List.copyOf(criticalFlows);
        priorityWaves = priorityWaves == null ? List.of() : List.copyOf(priorityWaves);
        learnings = learnings == null ? List.of() : List.copyOf(learnings);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }

    public record HotspotResponse(
            String slice,
            int mainFiles,
            int testFiles,
            int incomingDependencies,
            int outgoingDependencies,
            int controllerConsumers,
            double testRatio,
            int extractionPressure,
            String priority,
            List<String> signals,
            List<String> recommendedActions,
            List<ExtractionLaneResponse> extractionLanes
    ) {
        public HotspotResponse {
            signals = signals == null ? List.of() : List.copyOf(signals);
            recommendedActions = recommendedActions == null ? List.of() : List.copyOf(recommendedActions);
            extractionLanes = extractionLanes == null ? List.of() : List.copyOf(extractionLanes);
            priority = priority == null ? "MODERADA" : priority;
        }
    }

    public record ExtractionLaneResponse(
            String name,
            int mainFiles,
            int testFiles,
            double testRatio,
            String readiness,
            List<String> signals,
            List<String> initialActions
    ) {
        public ExtractionLaneResponse {
            readiness = readiness == null ? "PREPARAR" : readiness;
            signals = signals == null ? List.of() : List.copyOf(signals);
            initialActions = initialActions == null ? List.of() : List.copyOf(initialActions);
        }
    }

    public record ExtractionBlueprintResponse(
            String slice,
            String lane,
            String readiness,
            int priorityScore,
            String targetPackage,
            String suggestedFacade,
            String suggestedPort,
            String suggestedIntegrationContract,
            List<String> blockers,
            List<String> firstActions
    ) {
        public ExtractionBlueprintResponse {
            readiness = readiness == null ? "PREPARAR" : readiness;
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
            firstActions = firstActions == null ? List.of() : List.copyOf(firstActions);
        }
    }

    public record CriticalFlowResponse(
            String name,
            String status,
            double coverage,
            List<String> relatedTests,
            List<String> signals,
            List<String> initialActions
    ) {
        public CriticalFlowResponse {
            status = status == null ? "AUSENTE" : status;
            relatedTests = relatedTests == null ? List.of() : List.copyOf(relatedTests);
            signals = signals == null ? List.of() : List.copyOf(signals);
            initialActions = initialActions == null ? List.of() : List.copyOf(initialActions);
        }
    }
}
