package com.tcc.pjb.backend.model.dto.pastadigital;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageResolveResponse {
    String pageId;
    UUID documentoId;
    Long processoId;
    Integer pageNumber;
    String fingerprint;
    String texto;

    public static class PageResolveResponseBuilder {
        public PageResolveResponseBuilder documentoId(java.util.UUID documentoId) {
            this.documentoId = documentoId;
            return this;
        }

        public PageResolveResponseBuilder documentoId(Long documentoId) {
            this.documentoId = documentoId == null ? null : new UUID(0L, documentoId);
            return this;
        }
    }
}
