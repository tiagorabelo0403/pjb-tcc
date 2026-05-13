package com.tcc.pjb.backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.NaturezaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.TipoJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JurisdicaoRequest {

    @NotBlank(message = "Código é obrigatório")
    @Size(min = 3, max = 50)
    private String codigo;

    @NotBlank(message = "Sigla é obrigatória")
    @Size(min = 2, max = 20)
    private String sigla;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 5, max = 200)
    private String nome;

    @NotNull(message = "Tipo é obrigatório")
    private TipoJurisdicao tipo;

    @NotNull(message = "Natureza é obrigatória")
    private NaturezaJurisdicao natureza;

    @NotNull(message = "Grau é obrigatório")
    private GrauJurisdicao grau;

    @NotNull(message = "Esfera é obrigatória")
    private EsferaJurisdicao esfera;

    @NotNull(message = "Matéria é obrigatória")
    private MateriaJurisdicao materia;

    
    private String comarca;
    private String estado;
    private Double tetoValorCausa;
    private Boolean permiteJuizadoEspecial = false;
}