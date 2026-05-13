package com.tcc.pjb.backend.service.document.reading;

import java.util.List;

public record ProcessReadingModeProfile(
        String profileCode,
        String visualTheme,
        String glareControlMode,
        String contrastMode,
        String fontScale,
        String lineSpacing,
        String segmentationMode,
        String navigationMode,
        String evidenceMode,
        String recursalMode,
        String supportDeskMode,
        String noteMode,
        String fatigueShieldMode,
        String summaryMode,
        long totalDocumentos,
        long totalPaginas,
        int coberturaTextualPercentual,
        boolean sigiloReforcado,
        boolean recursal,
        boolean volumeExtenso,
        List<String> alerts
) {
    public ProcessReadingModeProfile {
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }
}
