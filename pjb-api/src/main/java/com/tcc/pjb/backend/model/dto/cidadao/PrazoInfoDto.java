package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;

public record PrazoInfoDto(
        Integer dias,
        String regime, 
        LocalDateTime inicio,
        LocalDateTime fim,
        Long diasRestantes,
        Boolean urgente
) {}
