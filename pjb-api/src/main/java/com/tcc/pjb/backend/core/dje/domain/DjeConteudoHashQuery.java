package com.tcc.pjb.backend.core.dje.domain;

public record DjeConteudoHashQuery(
        Long processoId,
        String tipoAto,
        String conteudoHash
) {}
