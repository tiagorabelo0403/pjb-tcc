package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.List;

public record LegalTranslatorResponse(
        String role,
        String titulo,
        String fonte,
        String original,
        String traduzido,
        String resumoExecutivo,
        List<String> proximosPassos,
        List<String> alertas,
        List<GlossaryItem> glossario,
        List<SourceRef> fontes,
        double confianca,
        Instant geradoEm
) {

    public record GlossaryItem(String termo, String explicacao) {
    }

    public record SourceRef(String tipo, String valor) {
    }
}
