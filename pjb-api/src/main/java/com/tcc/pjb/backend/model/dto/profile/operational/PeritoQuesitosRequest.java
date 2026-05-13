package com.tcc.pjb.backend.model.dto.profile.operational;

import java.util.Map;
import jakarta.validation.constraints.NotEmpty;

public record PeritoQuesitosRequest(
        @NotEmpty Map<Integer, String> respostas
) {}
