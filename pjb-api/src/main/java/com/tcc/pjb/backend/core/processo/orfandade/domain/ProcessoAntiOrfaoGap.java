package com.tcc.pjb.backend.core.processo.orfandade.domain;

import java.util.List;

public record ProcessoAntiOrfaoGap(
        String code,
        String axis,
        String severity,
        boolean blocking,
        String detail,
        List<String> correctiveActions
) {
    public ProcessoAntiOrfaoGap {
        code = code == null || code.isBlank() ? "ANTI_ORFAO_GAP" : code;
        axis = axis == null || axis.isBlank() ? "ARQUITETURA" : axis;
        severity = severity == null || severity.isBlank() ? "ALTA" : severity;
        detail = detail == null ? "" : detail;
        correctiveActions = correctiveActions == null ? List.of() : List.copyOf(correctiveActions);
    }
}
