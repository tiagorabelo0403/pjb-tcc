package com.tcc.pjb.backend.service.processual.participacao.submission;

import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;

public record PreparedAttachment(DocumentoProcessual documento, int sizeBytes) {}
