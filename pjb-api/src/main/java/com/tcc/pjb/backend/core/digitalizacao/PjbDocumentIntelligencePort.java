package com.tcc.pjb.backend.core.digitalizacao;

public interface PjbDocumentIntelligencePort {

    DocumentIntelligenceResult analyze(DocumentIntelligenceRequest request);
}
