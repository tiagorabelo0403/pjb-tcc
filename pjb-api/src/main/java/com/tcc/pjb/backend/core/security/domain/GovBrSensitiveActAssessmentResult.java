package com.tcc.pjb.backend.core.security.domain;

public record GovBrSensitiveActAssessmentResult(String tipoAto, boolean permitido, boolean exigeStepUp, String nivelRequerido) {}
