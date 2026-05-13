package com.tcc.pjb.backend.model.dto.juiz;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CertidaoTJResponse(
        Long processoId,
        String processoNumero,
        UUID documentoId,
        String hashDocumento,
        String pqcAlgorithm,
        String pqcSignatureB64,
        String pqcPublicKeyB64,
        Instant generatedAt,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana
) {
}
