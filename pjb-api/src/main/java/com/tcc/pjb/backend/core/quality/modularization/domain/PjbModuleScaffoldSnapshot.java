package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.time.Instant;
import java.util.List;

public record PjbModuleScaffoldSnapshot(
        boolean scaffoldPresent,
        boolean aggregatorLinked,
        int modulePomCount,
        int scaffoldDirectoryCount,
        List<String> nextSteps,
        Instant generatedAt
) {
}
