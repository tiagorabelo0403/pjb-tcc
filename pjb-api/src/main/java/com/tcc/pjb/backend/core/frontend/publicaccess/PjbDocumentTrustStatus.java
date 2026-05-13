package com.tcc.pjb.backend.core.frontend.publicaccess;

public enum PjbDocumentTrustStatus {
    TRUSTED,
    PUBLIC_REDACTION_REQUIRED,
    SIGNATURE_REVIEW_REQUIRED,
    TIMESTAMP_REVIEW_REQUIRED,
    HASH_MISMATCH,
    REVOKED
}
