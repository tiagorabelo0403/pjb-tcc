package com.tcc.pjb.backend.model.dto.expediente;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SemInteresseResponse(
        Long expedienteId,
        Long processoId,
        String processoNumero,
        String statusExpediente,
        boolean semInteresse,
        UUID documentoId,
        String hashDocumento,
        Instant registradoEm,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana
) {
}
