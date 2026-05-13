package com.tcc.pjb.backend.core.eleitoral.domain;

import java.time.LocalDate;

public record RegistrarDiplomacaoCommand(Long processoId, LocalDate dataDiplomacao) {
}
