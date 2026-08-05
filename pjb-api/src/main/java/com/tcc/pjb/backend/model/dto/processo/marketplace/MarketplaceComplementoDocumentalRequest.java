package com.tcc.pjb.backend.model.dto.processo.marketplace;

import com.tcc.pjb.backend.model.dto.Attachment;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MarketplaceComplementoDocumentalRequest(
        @NotEmpty List<Attachment> documentos
) {
}
