package com.tcc.pjb.backend.model.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class OrgaoJudiciarioResponse {
    private Long id;
    private String nome;
    private String sigla;
    private String tipo;
    private String comarca;
    private String estado;
    private boolean ativo;
    private LocalDateTime dataCriacao;
}