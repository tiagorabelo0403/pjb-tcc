package com.tcc.pjb.backend.core.criminal.custodia.domain;

public record CustodiaMandadoResult(
        Long processoId,
        String cpfConsultado,
        boolean mandadoAtivo,
        String numeroMandado,
        String status
) {}
