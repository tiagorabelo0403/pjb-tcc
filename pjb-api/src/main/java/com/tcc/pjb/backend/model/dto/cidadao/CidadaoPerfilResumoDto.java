package com.tcc.pjb.backend.model.dto.cidadao;

public record CidadaoPerfilResumoDto(
    String nome,
    String cpfMascarado,
    String uf,
    String avatarUrl,
    String avatarEtag,
    boolean govBrEnabled,
    String govBrLinkStartUrl
) {
}
