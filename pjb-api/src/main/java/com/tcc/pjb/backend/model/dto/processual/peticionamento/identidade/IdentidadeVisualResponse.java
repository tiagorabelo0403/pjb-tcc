package com.tcc.pjb.backend.model.dto.processual.peticionamento.identidade;

import com.tcc.pjb.backend.model.entity.peticionamento.PeticaoIdentidadeVisual;

public record IdentidadeVisualResponse(
        String escopo,
        String escopoRef,
        String nomeExibicao,
        String nomeInstituicao,
        boolean temLogo,
        String logoUrl,
        String logoSha256,
        String cabecalhoLivre,
        String rodapeLivre,
        String paletaPrimaria,
        String paletaSecundaria,
        boolean exibirRegistroProfissional,
        boolean exibirBrasaoLogomarca,
        boolean ativo
) {

    public static final String LOGO_URL = "/api/v1/peticionamento/identidade-visual/logo";

    public static IdentidadeVisualResponse from(PeticaoIdentidadeVisual entity) {
        boolean temLogo = entity.temLogo();
        return new IdentidadeVisualResponse(
                entity.getEscopo(),
                entity.getEscopoRef(),
                entity.getNomeExibicao(),
                entity.getNomeInstituicao(),
                temLogo,
                temLogo ? LOGO_URL : null,
                entity.getLogoSha256(),
                entity.getCabecalhoLivre(),
                entity.getRodapeLivre(),
                entity.getPaletaPrimaria(),
                entity.getPaletaSecundaria(),
                entity.isExibirRegistroProfissional(),
                entity.isExibirBrasaoLogomarca(),
                entity.isAtivo());
    }

    public static IdentidadeVisualResponse vazia() {
        return new IdentidadeVisualResponse("INDIVIDUAL", null, null, null, false, null, null,
                null, null, null, null, true, true, true);
    }
}
