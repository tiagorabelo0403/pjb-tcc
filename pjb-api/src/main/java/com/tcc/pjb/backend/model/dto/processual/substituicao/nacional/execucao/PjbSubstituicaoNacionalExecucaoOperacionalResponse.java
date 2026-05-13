package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao;

import com.tcc.pjb.backend.model.dto.processual.substituicao.comunicacao.PjbSubstituicaoComunicacaoSyncCursorResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.homologacao.PjbSubstituicaoHomologacaoProbeResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.migracao.PjbSubstituicaoMigracaoLoteResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalOperacionalResumoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal.PjbSubstituicaoTribunalReconciliacaoResponse;
import java.util.List;

public record PjbSubstituicaoNacionalExecucaoOperacionalResponse(
        PjbSubstituicaoNacionalExecucaoResponse execucao,
        PjbSubstituicaoNacionalOperacionalResumoResponse resumo,
        List<PjbSubstituicaoHomologacaoProbeResponse> probes,
        List<PjbSubstituicaoMigracaoLoteResponse> migracaoLotes,
        List<PjbSubstituicaoComunicacaoSyncCursorResponse> comunicacaoCursores,
        PjbSubstituicaoTribunalReconciliacaoResponse reconciliacaoTribunal
) {
    public PjbSubstituicaoNacionalExecucaoOperacionalResponse {
        probes = probes == null ? List.of() : List.copyOf(probes);
        migracaoLotes = migracaoLotes == null ? List.of() : List.copyOf(migracaoLotes);
        comunicacaoCursores = comunicacaoCursores == null ? List.of() : List.copyOf(comunicacaoCursores);
    }
}
