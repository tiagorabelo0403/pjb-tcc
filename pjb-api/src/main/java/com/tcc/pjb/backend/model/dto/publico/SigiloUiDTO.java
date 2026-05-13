package com.tcc.pjb.backend.model.dto.publico;


public record SigiloUiDTO(
        boolean sigiloso,
        int nivel,
        String label,
        String icon,
        String color,
        String mensagemPublica
) {
}
