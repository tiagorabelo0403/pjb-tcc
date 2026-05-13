package com.tcc.pjb.backend.core.security.domain;

public record GovBrAssuranceResult(String nivelAtual, String nivelRequerido, boolean atendido, boolean exigeStepUp) {}
