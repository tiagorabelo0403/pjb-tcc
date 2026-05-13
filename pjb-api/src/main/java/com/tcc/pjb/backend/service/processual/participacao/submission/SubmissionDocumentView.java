package com.tcc.pjb.backend.service.processual.participacao.submission;

public record SubmissionDocumentView(String documentoId,
                                     String titulo,
                                     String contentType,
                                     Long tamanhoBytes,
                                     String categoria,
                                     String nivelSigilo,
                                     String storageUri) {
}
