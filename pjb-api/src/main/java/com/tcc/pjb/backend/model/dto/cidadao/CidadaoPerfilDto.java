package com.tcc.pjb.backend.model.dto.cidadao;

public record CidadaoPerfilDto(
    String nome,
    String email,
    String cpfMascarado,
    String uf,
    String avatarUrl,
    String avatarEtag,
    boolean govBrEnabled,
    String govBrLinkStartUrl
) {
}
