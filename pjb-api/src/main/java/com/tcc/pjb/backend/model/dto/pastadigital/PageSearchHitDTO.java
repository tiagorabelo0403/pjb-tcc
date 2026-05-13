package com.tcc.pjb.backend.model.dto.pastadigital;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageSearchHitDTO {
    String pageId;
    UUID documentoId;
    Integer pageNumber;
    String fingerprint;
    String preview;

    public static class PageSearchHitDTOBuilder {
        public PageSearchHitDTOBuilder documentoId(java.util.UUID documentoId) {
            this.documentoId = documentoId;
            return this;
        }

        public PageSearchHitDTOBuilder documentoId(Long documentoId) {
            this.documentoId = documentoId == null ? null : new UUID(0L, documentoId);
            return this;
        }
    }
}
