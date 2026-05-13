package com.tcc.pjb.backend.model.dto.profile.operational;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public record AdvogadoCienciaLoteRequest(
        @NotEmpty List<Long> workItemIds
) {}
