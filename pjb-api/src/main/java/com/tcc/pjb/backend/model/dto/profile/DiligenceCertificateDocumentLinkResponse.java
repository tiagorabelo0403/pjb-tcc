package com.tcc.pjb.backend.model.dto.profile;

import java.time.Instant;
import java.util.UUID;

public record DiligenceCertificateDocumentLinkResponse(
        Long vinculoId,
        Long certidaoId,
        Long processoId,
        UUID documentoId,
        String documentoTitulo,
        String documentoSha256,
        String origem,
        Instant createdAt
) {
}
