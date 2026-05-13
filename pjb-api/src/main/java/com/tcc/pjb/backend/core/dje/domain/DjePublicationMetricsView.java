package com.tcc.pjb.backend.core.dje.domain;

public record DjePublicationMetricsView(
        int pending,
        int sent,
        int published,
        int failed
) {}
