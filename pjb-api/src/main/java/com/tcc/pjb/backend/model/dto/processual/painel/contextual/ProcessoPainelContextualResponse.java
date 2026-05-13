package com.tcc.pjb.backend.model.dto.processual.painel.contextual;

import java.time.Instant;
import java.util.List;

public record ProcessoPainelContextualResponse(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        String painelCodigo,
        String painelTitulo,
        String perfilCodigo,
        List<ProcessoPainelContextualWidgetResponse> widgets,
        List<ProcessoPainelConectorSaudeResponse> conectores,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoPainelContextualResponse {
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        conectores = conectores == null ? List.of() : List.copyOf(conectores);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
