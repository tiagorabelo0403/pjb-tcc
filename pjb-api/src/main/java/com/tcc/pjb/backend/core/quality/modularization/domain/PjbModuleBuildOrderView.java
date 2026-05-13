package com.tcc.pjb.backend.core.quality.modularization.domain;

import java.util.List;

public record PjbModuleBuildOrderView(
        int step,
        String moduleName,
        String status,
        String rationale,
        List<String> prerequisites
) {
}
