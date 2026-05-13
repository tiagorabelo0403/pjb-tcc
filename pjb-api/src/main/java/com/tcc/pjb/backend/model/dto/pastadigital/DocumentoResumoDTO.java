package com.tcc.pjb.backend.model.dto.pastadigital;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentoResumoDTO {
    UUID documentoId;
    String nomeOriginal;
    String titulo;
    String contentType;
    Long tamanhoBytes;
    Integer numeroPaginas;
    LocalDateTime criadoEm;

    public static class DocumentoResumoDTOBuilder {
        public DocumentoResumoDTOBuilder documentoId(java.util.UUID documentoId) {
            this.documentoId = documentoId;
            return this;
        }

        public DocumentoResumoDTOBuilder documentoId(Long documentoId) {
            this.documentoId = documentoId == null ? null : new UUID(0L, documentoId);
            return this;
        }
    }
}
