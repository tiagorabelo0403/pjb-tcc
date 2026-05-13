package com.tcc.pjb.backend.model.dto.oab;

import jakarta.validation.constraints.*;
import com.tcc.pjb.backend.model.entity.enums.TipoEventoInstitucional;
import lombok.Data;

@Data
public class OabEventoCreateRequest {

    @NotNull
    private TipoEventoInstitucional tipo;

    @NotBlank
    @Size(min = 2, max = 2)
    private String uf;

    @Min(1)
    @Max(5)
    private int severidade = 3;

    @NotBlank
    @Size(max = 240)
    private String resumo;

    @Size(max = 4000)
    private String detalhes;

    @Size(max = 40)
    private String tribunal;

    @Size(max = 120)
    private String orgao;

    private Long processoId;

    @Size(max = 40)
    private String numeroProcesso;
}
