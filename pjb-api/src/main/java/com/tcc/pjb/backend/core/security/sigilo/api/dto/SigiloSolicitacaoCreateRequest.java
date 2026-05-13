package com.tcc.pjb.backend.core.security.sigilo.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SigiloSolicitacaoCreateRequest {

    @NotNull
    private Long processoId;

    private String motivo;
}
