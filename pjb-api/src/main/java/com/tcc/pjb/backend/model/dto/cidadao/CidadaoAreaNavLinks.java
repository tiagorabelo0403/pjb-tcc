package com.tcc.pjb.backend.model.dto.cidadao;

public record CidadaoAreaNavLinks(
        String painelUrl,
        String carteiraUrl,
        String pendenciasUrl,
        String meusProcessosUrl,
        String pastaProcessosUrl,
        String audienciasUrl,
        String liveStreamUrl,
        String govHubUrl,
        String publicSearchUrl,
        String publicPageUrlTemplate
) {
}
