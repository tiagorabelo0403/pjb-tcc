package com.tcc.pjb.backend.model.dto.cidadao.surface;

import java.time.Instant;
import java.time.LocalDateTime;

public record CidadaoResumoProcessoResponse(
        String numero,
        String tribunal,
        String comarca,
        String faseAtual,
        LocalDateTime dataDistribuicao,
        String rito,
        Instant geradoEm,
        String assinaturaDigital
) {}
