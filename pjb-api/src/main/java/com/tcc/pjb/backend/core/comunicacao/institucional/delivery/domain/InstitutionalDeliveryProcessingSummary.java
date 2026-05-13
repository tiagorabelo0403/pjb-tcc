package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain;

public record InstitutionalDeliveryProcessingSummary(
        int selected,
        int delivered,
        int handedOff,
        int retried,
        int deadLettered
) {
}
