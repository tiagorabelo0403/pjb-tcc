package com.tcc.pjb.backend.modules.atendimento.dto;

public record AtendimentoAttachmentDto(
        Long id,
        String fileName,
        String contentType,
        long sizeBytes,
        String status
) {
}
