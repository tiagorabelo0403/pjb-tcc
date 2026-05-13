package com.tcc.pjb.backend.model.dto.leitura;

public record ProcessReadingSummaryResponse(
        Long processoId,
        String numeroProcesso,
        String tribunal,
        String ramo,
        String materia,
        String faseAtual,
        String rito,
        String profileCode,
        String presetCode,
        String readingIntensity,
        String visualTheme,
        String contrastMode,
        String fontScale,
        String lineSpacing,
        String navigationMode,
        String chronologyMode,
        String citationMode,
        int maxWidthCh,
        int chunkPageSize,
        long totalDocumentos,
        long totalPaginas,
        int coberturaTextualPercentual,
        boolean sigiloReforcado,
        boolean recursal,
        boolean volumeExtenso,
        long totalEntradasProcessuais
) {
}
