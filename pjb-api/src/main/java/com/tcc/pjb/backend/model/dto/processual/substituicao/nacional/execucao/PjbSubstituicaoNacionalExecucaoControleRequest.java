package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoControleAcao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PjbSubstituicaoNacionalExecucaoControleRequest(
        @NotNull PjbSubstituicaoExecucaoControleAcao acao,
        @Size(max = 500) String motivo
) {
}
