package com.tcc.pjb.backend.model.dto.intelligence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PessoaLocalizacaoRequest(
        @NotBlank(message = "cpf é obrigatório")
        String cpf,

        Long processoId,

        Long mandadoId,

        PessoaLocalizacaoFundamento fundamento,

        @Size(max = 500, message = "finalidade muito longa")
        String finalidade,

        @Size(max = 1000, message = "justificativa operacional muito longa")
        String justificativaOperacional,

        @Size(max = 120, message = "referência procedimental muito longa")
        String referenciaProcedimental,

        boolean incluirProntuario,
        boolean incluirRestricoes,
        boolean incluirMandados,
        boolean exigirEnderecoEstrito
) {
}
