package com.tcc.pjb.backend.core.security.professional;

public enum ProfessionalDocumentVisibilityScope {
    PUBLIC_ACT,
    PUBLIC_DOCUMENT,
    PROFESSIONAL_NON_MANDATE_VIEW,
    COUNSEL_REPRESENTED_PARTY,
    INSTITUTIONAL_REPRESENTATION,
    COURT_INTERNAL,
    CHAMBER_INTERNAL,
    PRIVATE_DRAFT,
    EVIDENCE_RESTRICTED;

    public String displayName() {
        return switch (this) {
            case PUBLIC_ACT -> "Ato público";
            case PUBLIC_DOCUMENT -> "Documento público";
            case PROFESSIONAL_NON_MANDATE_VIEW -> "Documento visível ao profissional sem mandato";
            case COUNSEL_REPRESENTED_PARTY -> "Documento restrito ao representante habilitado";
            case INSTITUTIONAL_REPRESENTATION -> "Documento restrito à representação institucional";
            case COURT_INTERNAL -> "Documento interno do juízo";
            case CHAMBER_INTERNAL -> "Documento interno do colegiado";
            case PRIVATE_DRAFT -> "Rascunho privado não protocolado";
            case EVIDENCE_RESTRICTED -> "Prova restrita";
        };
    }
}
