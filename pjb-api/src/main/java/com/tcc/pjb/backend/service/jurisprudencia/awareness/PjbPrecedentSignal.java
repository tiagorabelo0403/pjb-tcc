package com.tcc.pjb.backend.service.jurisprudencia.awareness;

public record PjbPrecedentSignal(PjbPrecedentSignalType type,
                                 String reference,
                                 boolean binding,
                                 boolean suspensionRecommended,
                                 String rationale) {
}
