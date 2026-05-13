package com.tcc.pjb.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgaoJudiciarioRequest {

    @NotBlank(message = "Nome do órgão é obrigatório")
    @Size(min = 5, max = 255)
    private String nome;

    @NotBlank(message = "Sigla do órgão é obrigatória")
    @Size(min = 2, max = 30)
    private String sigla;

    @NotBlank(message = "Tipo de órgão é obrigatório")
    private String tipo; 

    private String comarca;

    @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres")
    private String estado;
}