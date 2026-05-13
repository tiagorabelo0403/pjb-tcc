package com.tcc.pjb.backend.service.processual.participacao.submission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttachmentRequest(@NotBlank @Size(max = 220) String nomeArquivo,
                                @NotBlank @Size(max = 120) String contentType,
                                @NotBlank String base64Content,
                                @Size(max = 220) String titulo,
                                @Size(max = 40) String categoria,
                                @Size(max = 40) String nivelSigilo) {
}
