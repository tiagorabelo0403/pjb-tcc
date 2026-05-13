package com.tcc.pjb.backend.model.dto.pastadigital;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DocumentoIndexadoResponse {
    UUID documentoId;
    Long processoId;
    String nomeOriginal;
    String titulo;
    String contentType;
    Long tamanhoBytes;
    String sha256;
    Integer numeroPaginas;
    List<PageRefDTO> pages;

    public static class DocumentoIndexadoResponseBuilder {
        public DocumentoIndexadoResponseBuilder documentoId(java.util.UUID documentoId) {
            this.documentoId = documentoId;
            return this;
        }

        public DocumentoIndexadoResponseBuilder documentoId(Long documentoId) {
            this.documentoId = documentoId == null ? null : new UUID(0L, documentoId);
            return this;
        }
    }
}
