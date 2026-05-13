package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaMandadoAuditView(
        Long processoId,
        String cpfConsultado,
        boolean mandadoAtivo,
        Instant consultadoEm
) {}
