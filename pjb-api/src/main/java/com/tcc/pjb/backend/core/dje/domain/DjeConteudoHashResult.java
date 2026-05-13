package com.tcc.pjb.backend.core.dje.domain;

public record DjeConteudoHashResult(
        Long processoId,
        String tipoAto,
        String conteudoHash,
        boolean exists,
        String status
) {}
