package com.tcc.pjb.backend.service.processual.participacao.submission;

import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;

public record PreparedPrimaryDocument(DocumentoProcessual documento, int sizeBytes) {}
