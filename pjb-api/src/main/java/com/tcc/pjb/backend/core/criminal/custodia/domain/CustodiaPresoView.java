package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaPresoView(
        Long processoId,
        String presoNome,
        String presoCpf,
        Instant dataPrisao
) {}
