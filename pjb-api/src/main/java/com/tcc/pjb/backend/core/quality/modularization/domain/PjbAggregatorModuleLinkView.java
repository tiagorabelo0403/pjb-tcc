package com.tcc.pjb.backend.core.quality.modularization.domain;

public record PjbAggregatorModuleLinkView(
        String moduleName,
        String relativePomPath,
        boolean directoryPresent,
        boolean pomPresent,
        boolean listedInAggregatorFile,
        boolean listedInRootPom) {
}
