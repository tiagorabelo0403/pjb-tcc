package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DesembargadorVistaRequest(
        @Min(1) @Max(180) int diasVista
) {}
