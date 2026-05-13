package com.tcc.pjb.backend.service.rito.dto;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;

public record AdvanceRitoRequest(
        FaseProcessual nextFase,
        String motivo
) {}
