package com.tcc.pjb.backend.modules.laiane.dto.roles.mp;

public record LaianeDocumentoFormalAssinadoResponse(
        String tituloDocumento,
        String conteudoAssinado,
        String hashSha256,
        boolean selado,
        LaianeAssinaturaQualificadaResponse assinaturaQualificada,
        LaianeValidacaoSoberanaResponse validacaoSoberana
) {
}
