package com.tcc.pjb.backend.core.security.accesskey;

import java.util.List;

public record PjbProcessAccessKeyDecision(
        boolean allowed,
        String code,
        List<String> reasons
) {
    public PjbProcessAccessKeyDecision {
        code = code == null || code.isBlank() ? (allowed ? "ACCESS_ALLOWED" : "ACCESS_DENIED") : code.trim();
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
