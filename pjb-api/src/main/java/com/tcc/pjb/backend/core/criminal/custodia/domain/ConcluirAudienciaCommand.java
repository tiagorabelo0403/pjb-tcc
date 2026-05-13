package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.util.List;

public record ConcluirAudienciaCommand(Long custodiaId,
                                       String resultado,
                                       List<String> medidasCautelares) {
}
