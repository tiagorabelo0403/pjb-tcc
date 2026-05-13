package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.Map;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

public record ExternalProcessSnapshot(
        JudicialSystem system,
        String numeroUnificado,
        String classeProcessual,
        String assunto,
        NivelSigilo nivelSigilo,
        Instant fetchedAt,
        Map<String, Object> raw
) {
}
