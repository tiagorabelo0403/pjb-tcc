package com.tcc.pjb.backend.model.dto.processo.marketplace;

import com.tcc.pjb.backend.model.dto.Attachment;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MarketplaceComplementoDocumentalRequest(
        @NotEmpty @Size(max = 5) List<Attachment> documentos
) {
}
