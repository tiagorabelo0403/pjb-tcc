package com.tcc.pjb.backend.core.quality.codebase.application;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PjbCodebaseSanitySettings(
        Set<String> allowlistedVirtualThreadFiles,
        Set<String> allowlistedLegacyJudgeImports,
        Set<String> orphanDirectories,
        Set<String> blockingIssueCodes,
        Duration cacheTtl
) {

    public PjbCodebaseSanitySettings {
        allowlistedVirtualThreadFiles = normalize(allowlistedVirtualThreadFiles);
        allowlistedLegacyJudgeImports = normalize(allowlistedLegacyJudgeImports);
        orphanDirectories = normalize(orphanDirectories);
        blockingIssueCodes = normalize(blockingIssueCodes);
        cacheTtl = cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero() ? Duration.ofMinutes(5) : cacheTtl;
    }

    public static PjbCodebaseSanitySettings defaults() {
        return new PjbCodebaseSanitySettings(
                Set.of(
                        "PjbVirtualThreadSpine.java",
                        "PjbCodebaseSanitySnapshotBuilder.java"
                ),
                Set.of(
                        "src/main/java/com/tcc/pjb/backend/controller/JudgeDocketController.java",
                        "pjb-api/src/main/java/com/tcc/pjb/backend/controller/JudgeDocketController.java",
                        "pjb-api/src/test/java/com/tcc/pjb/backend/governance/layout/LegacyJudgePackageIsolationGovernanceTest.java"
                ),
                Set.of("out", "outcheck"),
                Set.of(
                        "repository.entity.quebrada",
                        "legacy.judge.import",
                        "legacy.judge.package.expansion",
                        "quality.coverage.absent",
                        "quality.checkstyle.absent",
                        "quality.checkstyle.config.absent"
                ),
                Duration.ofMinutes(5)
        );
    }

    private static Set<String> normalize(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = Objects.toString(value, "").trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return Set.copyOf(List.copyOf(normalized));
    }
}
