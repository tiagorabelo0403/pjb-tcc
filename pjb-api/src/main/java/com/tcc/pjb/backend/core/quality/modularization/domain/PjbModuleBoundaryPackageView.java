package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.List;

public record PjbModuleBoundaryPackageView(
        String moduleName,
        int fileCount,
        List<String> topPackages
) {
    public PjbModuleBoundaryPackageView {
        topPackages = topPackages == null ? List.of() : List.copyOf(topPackages);
    }
}
