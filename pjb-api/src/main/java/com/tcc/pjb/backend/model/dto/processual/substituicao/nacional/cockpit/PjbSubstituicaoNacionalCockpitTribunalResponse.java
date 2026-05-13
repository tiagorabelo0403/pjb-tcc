package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoNacionalCockpitTribunalResponse(
        String tribunalCodigo,
        String tribunalNome,
        String ondaAlvo,
        PjbSubstituicaoExecucaoAcao ultimaAcao,
        PjbSubstituicaoExecucaoSituacao ultimaSituacao,
        PjbSubstituicaoExecucaoFase ultimaFase,
        int gateScore,
        boolean cutoverPronto,
        boolean rollbackReversivel,
        String homologacaoStatus,
        String migracaoStatus,
        String comunicacaoStatus,
        String cutoverStatus,
        String rollbackStatus,
        List<String> bloqueadores,
        Instant atualizadoEm
) {
    public PjbSubstituicaoNacionalCockpitTribunalResponse {
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
    }
}
