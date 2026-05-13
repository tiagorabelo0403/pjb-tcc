package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;

public record CidadaoAudienciaDto(
        Long audienciaId,
        Long processoId,
        String numeroUnificado,
        String tipo,
        String modalidade,
        String status,
        LocalDateTime dataHora,
        Integer duracaoMin,
        String local,
        String linkVideo,
        String pauta
) {}
