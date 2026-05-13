package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import java.time.Instant;
import java.util.UUID;

public record PjbSubstituicaoNacionalExecucaoCommandResponse(
        Long execucaoId,
        String tribunalCodigo,
        PjbSubstituicaoExecucaoAcao acao,
        PjbSubstituicaoExecucaoSituacao situacao,
        UUID jobId,
        boolean replay,
        boolean inProgress,
        String correlationId,
        Instant geradoEm
) {
    public PjbSubstituicaoNacionalExecucaoCommandResponse {
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
