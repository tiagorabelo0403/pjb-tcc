package com.tcc.pjb.backend.service.processual.participacao.submission;

import java.time.Instant;
import java.util.List;

public record SubmissionView(String documentoId,
                             String titulo,
                             String acao,
                             Instant criadoEm,
                             String contentType,
                             Long tamanhoBytes,
                             String nivelSigilo,
                             String storageUri,
                             List<String> tags) {
}
