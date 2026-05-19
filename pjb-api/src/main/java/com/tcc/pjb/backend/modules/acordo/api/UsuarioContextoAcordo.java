package com.tcc.pjb.backend.modules.acordo.api;

import java.util.List;

public record UsuarioContextoAcordo(
        Long usuarioId,
        String nomeExibicao,
        List<String> papeis,
        boolean ativo,
        boolean podeParticipar,
        boolean podeHomologar
) {
}
