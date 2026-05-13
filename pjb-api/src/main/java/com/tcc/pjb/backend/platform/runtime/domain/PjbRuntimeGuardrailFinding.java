package com.tcc.pjb.backend.platform.runtime.domain;

public record PjbRuntimeGuardrailFinding(String code,
                                         String severity,
                                         String scope,
                                         String summary,
                                         String recommendedAction) {
}
