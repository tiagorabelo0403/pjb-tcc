package com.tcc.pjb.backend.model.dto.ajuizamento.federal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.model.entity.federalismo.StatusNoFederacao;

public record FederalismoHeartbeatRequest(
        @NotBlank String codigoTribunal,
        @NotNull StatusNoFederacao status,
        long versaoSchemaAtual
) {
}
