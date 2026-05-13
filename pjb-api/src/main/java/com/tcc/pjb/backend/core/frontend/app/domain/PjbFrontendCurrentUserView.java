package com.tcc.pjb.backend.core.frontend.app.domain;

import java.util.List;

public record PjbFrontendCurrentUserView(
        Long userId,
        String nome,
        String email,
        String cpfMasked,
        String tipoUsuario,
        String perfil,
        String papelArquitetural,
        String uf,
        String comarca,
        boolean ativo,
        String govBrAssuranceLevel,
        boolean stepUpRequiredForSensitiveAct,
        List<String> authorities
) {
}
