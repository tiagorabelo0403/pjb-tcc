package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.UUID;

public record CidadaoDocumentoMetaDto(
    UUID documentoId,
    String titulo,
    String nomeOriginal,
    String contentType,
    Long tamanhoBytes,
    LocalDateTime criadoEm,
    String categoria,
    String nivelSigilo,
    String sha256,
    boolean canDownload,
    boolean requiresStepUp
) {
}
