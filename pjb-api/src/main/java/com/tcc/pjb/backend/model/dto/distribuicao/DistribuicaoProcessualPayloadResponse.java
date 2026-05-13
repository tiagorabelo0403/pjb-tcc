package com.tcc.pjb.backend.model.dto.distribuicao;

import java.util.Map;

public record DistribuicaoProcessualPayloadResponse(
        String scope,
        Map<String, Object> payload
) {
}
