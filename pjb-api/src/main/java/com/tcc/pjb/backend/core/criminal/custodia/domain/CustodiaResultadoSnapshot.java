package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record CustodiaResultadoSnapshot(Long custodiaId,
                                        String resultado,
                                        Instant realizadaEm) {}
