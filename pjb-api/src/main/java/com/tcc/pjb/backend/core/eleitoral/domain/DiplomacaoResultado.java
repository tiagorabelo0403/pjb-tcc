package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record DiplomacaoResultado(Long processoId,
                                  LocalDate dataDiplomacao,
                                  String zonaEleitoral,
                                  String uf) {
}
