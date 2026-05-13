package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitutionalRecursoRequest(
        @NotBlank @Size(max = 120) String tipoRecurso,
        @NotBlank String razoes,
        @NotBlank @Size(max = 4000) String fundamentacao,
        boolean pedidoEfeitoSuspensivo,
        boolean preparoDispensado,
        @Size(max = 2000) String observacoes
) {}
