package com.tcc.pjb.backend.model.dto.processual.peticionamento.identidade;

import jakarta.validation.constraints.Size;

public record IdentidadeVisualRequest(
        @Size(max = 600) String nomeExibicao,
        @Size(max = 600) String nomeInstituicao,
        @Size(max = 1500) String cabecalhoLivre,
        @Size(max = 1500) String rodapeLivre,
        @Size(max = 16) String paletaPrimaria,
        @Size(max = 16) String paletaSecundaria,
        Boolean exibirRegistroProfissional,
        Boolean exibirBrasaoLogomarca
) {
}
