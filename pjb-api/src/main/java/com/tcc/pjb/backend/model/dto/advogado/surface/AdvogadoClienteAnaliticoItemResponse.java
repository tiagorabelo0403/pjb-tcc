package com.tcc.pjb.backend.model.dto.advogado.surface;

public record AdvogadoClienteAnaliticoItemResponse(
        Long processoId,
        String numero,
        String fase,
        String rito,
        String tribunal
) {}
