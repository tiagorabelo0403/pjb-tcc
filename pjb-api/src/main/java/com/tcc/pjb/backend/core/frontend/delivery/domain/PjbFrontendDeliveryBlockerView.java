package com.tcc.pjb.backend.core.frontend.delivery.domain;

public record PjbFrontendDeliveryBlockerView(
        String scope,
        String severity,
        String code,
        String summary
) {
}
