package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;

public record ChainOfCustodySyncReplayResponse(
        String chaveCustodia,
        boolean assinaturaOk,
        boolean digestOk,
        boolean integridadeEstruturalOk,
        boolean correspondenciaLocalOk,
        long totalEntradas,
        String payloadDigestSha256,
        Instant processadoEm
) {
}
