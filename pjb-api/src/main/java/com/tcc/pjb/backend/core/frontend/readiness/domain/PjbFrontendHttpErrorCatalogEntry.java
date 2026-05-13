package com.tcc.pjb.backend.core.frontend.readiness.domain;

public record PjbFrontendHttpErrorCatalogEntry(
        int status,
        String code,
        String type,
        boolean retriable,
        String source
) {
}
