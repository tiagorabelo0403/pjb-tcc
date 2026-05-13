package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjePrazoProjection(LocalDate disponibilizacao,
                                 LocalDate publicacao,
                                 LocalDate prazoComecaEm) {
}
