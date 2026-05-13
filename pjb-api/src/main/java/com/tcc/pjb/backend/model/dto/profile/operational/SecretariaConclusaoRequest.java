package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaConclusaoRequest(
        @NotBlank String motivoConclusao
) {}
