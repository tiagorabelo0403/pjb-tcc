package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoEngineSnapshot(
        String engine,
        boolean enabled,
        String idiomaPadrao
) {}
