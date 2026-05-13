package com.tcc.pjb.backend.model.dto.ui;

public record WcagAaaAuditRequest(
        double contrastRatio,
        double keyboardShortcutCoverage,
        double ariaLiveCoverage,
        boolean vLibrasAtivo,
        boolean modoDislexiaAtivo,
        boolean focusAppearanceVisible,
        boolean readingLevelSimplified
) {
}
