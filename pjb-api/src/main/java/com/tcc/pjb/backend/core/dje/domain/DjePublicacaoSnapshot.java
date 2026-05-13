package com.tcc.pjb.backend.core.dje.domain;

import java.time.LocalDate;

public record DjePublicacaoSnapshot(Long djeId,
                                    Long processoId,
                                    String status,
                                    String tipoAto,
                                    LocalDate dataPublicacao,
                                    LocalDate prazoComecaEm) {
}
