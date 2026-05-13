package com.tcc.pjb.backend.ai.legalai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DreamingSessionRequest(
        @NotBlank String inputStoreId,
        List<String> sessionIds,
        @Size(max = 4096) String instrucoes,
        String modelo
) {}
