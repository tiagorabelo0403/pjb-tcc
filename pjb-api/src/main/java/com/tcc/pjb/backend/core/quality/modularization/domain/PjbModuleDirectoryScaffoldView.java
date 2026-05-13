package com.tcc.pjb.backend.core.quality.modularization.domain;

public record PjbModuleDirectoryScaffoldView(
        String moduleName,
        String path,
        boolean present
) {
}
