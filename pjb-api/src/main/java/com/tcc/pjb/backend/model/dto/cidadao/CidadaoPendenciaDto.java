package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoPendenciaDto(
        String tipo,                 
        int prioridade,              
        boolean urgente,
        LocalDateTime quando,        
        Long processoId,
        String numeroUnificado,
        String titulo,
        String resumo,
        List<String> uiTokens,
        Links links
) {}
