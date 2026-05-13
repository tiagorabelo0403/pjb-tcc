package com.tcc.pjb.backend.core.quality.modularization.domain;

public record PjbModulePomScaffoldView(
        String moduleName,
        String path,
        boolean present,
        String packaging,
        String artifactId,
        String parentArtifactId
) {
}
