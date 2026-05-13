package com.tcc.pjb.backend.model.dto.oab;

import jakarta.validation.constraints.NotNull;
import com.tcc.pjb.backend.model.entity.enums.StatusEventoInstitucional;
import lombok.Data;

@Data
public class OabEventoStatusUpdateRequest {

    @NotNull
    private StatusEventoInstitucional status;
}
