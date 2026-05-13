package com.tcc.pjb.backend.core.digitalizacao.domain;

public record RegistrarRevisaoPaginaCommand(
        Long paginaId,
        String conteudoOcrRevisado,
        String tipoPecaRevisado
) {
}
