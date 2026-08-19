package com.tcc.pjb.backend.model.dto.cidadao;

import java.math.BigDecimal;
import java.util.UUID;

public record CidadaoGratuidadeAvaliacaoRequest(
        UUID processoId,
        String parteId,
        boolean declaracaoHipossuficiencia,
        BigDecimal rendaMensalDeclarada,
        boolean representadoPorDefensoria,
        boolean beneficioSocial,
        boolean impugnadaPelaParteContraria
) {
}
