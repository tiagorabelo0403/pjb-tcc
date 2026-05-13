package com.tcc.pjb.backend.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    private String nome;
    @NotBlank
    private String cpf;
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String perfil;
    private String oab;
    private String matricula;
    private String orgao;
    private String comarca;
    private String jurisdicao;
}