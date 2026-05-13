package com.tcc.pjb.backend.model.dto.oab;

import jakarta.validation.constraints.*;
import com.tcc.pjb.backend.model.entity.enums.TipoProvidenciaInstitucional;
import lombok.Data;

@Data
public class OabProvidenciaCreateRequest {

    @NotNull
    private TipoProvidenciaInstitucional tipo;

    @NotBlank
    @Size(max = 240)
    private String titulo;

    @Size(max = 6000)
    private String descricao;
}
