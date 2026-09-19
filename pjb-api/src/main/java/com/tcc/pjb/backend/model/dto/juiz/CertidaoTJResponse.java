package com.tcc.pjb.backend.model.dto.juiz;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CertidaoTJResponse(
        Long processoId,
        String processoNumero,
        UUID documentoId,
        String hashDocumento,
        @Schema(description = "Algoritmo PQC da evidência experimental de agilidade criptográfica", example = "ML-DSA-87")
        String pqcAlgorithm,
        @Schema(description = "Evidência PQC em Base64, gerada com chave efêmera do ato, sem cadeia de "
                + "certificação e sem valor probatório na forma do art. 1º da MP 2.200-2/2001; a assinatura "
                + "com validade jurídica é a ICP-Brasil")
        String pqcSignatureB64,
        @Schema(description = "Chave pública efêmera que acompanha a evidência PQC, descartada após o ato")
        String pqcPublicKeyB64,
        Instant generatedAt,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana
) {
}
