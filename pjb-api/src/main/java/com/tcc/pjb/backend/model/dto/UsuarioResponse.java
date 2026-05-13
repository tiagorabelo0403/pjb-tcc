package com.tcc.pjb.backend.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class UsuarioResponse {
    private Long id;
    private String nome;
    private String email;
    private String perfil; 
    private String oab;
    private String matricula;
    private String comarca;
    private boolean ativo;
    private LocalDateTime dataCadastro;
    private UUID identidadeJuridicaId;
    private String documentoMascarado;
    private String nomeCanonicoIdentidade;
    private String prontuarioNacionalUri;
    private String nivelConfiancaIdentidade;
    private String receitaStatusIdentidade;
    private String oabStatusIdentidade;
    private boolean govBrVinculado;
}