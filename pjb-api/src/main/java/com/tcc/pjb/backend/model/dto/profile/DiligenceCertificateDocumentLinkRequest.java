package com.tcc.pjb.backend.model.dto.profile;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;

public record DiligenceCertificateDocumentLinkRequest(
        @NotEmpty List<UUID> documentoIds
) {
}
