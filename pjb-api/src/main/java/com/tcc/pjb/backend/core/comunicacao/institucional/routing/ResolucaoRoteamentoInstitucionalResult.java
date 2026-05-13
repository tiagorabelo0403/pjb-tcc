package com.tcc.pjb.backend.core.comunicacao.institucional.routing;

import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.AlvoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;

public record ResolucaoRoteamentoInstitucionalResult(
        AlvoInstitucional alvo,
        TipoComunicacaoJudicial tipoComunicacaoEfetiva,
        PlanoEntregaInstitucional planoEntrega,
        int slaCienciaHoras,
        int slaRespostaHoras,
        String gateCode,
        boolean bloqueiaFluxo,
        List<String> justificativas,
        String hashResolucao,
        String catalogVersion
) {
    public ResolucaoRoteamentoInstitucionalResult {
        if (alvo == null) {
            throw new IllegalArgumentException("alvo é obrigatório");
        }
        if (tipoComunicacaoEfetiva == null) {
            throw new IllegalArgumentException("tipoComunicacaoEfetiva é obrigatório");
        }
        if (planoEntrega == null) {
            throw new IllegalArgumentException("planoEntrega é obrigatório");
        }
        if (hashResolucao == null || hashResolucao.isBlank()) {
            throw new IllegalArgumentException("hashResolucao é obrigatório");
        }
        justificativas = PayloadMaps.copyDistinctStrings(justificativas);
        hashResolucao = hashResolucao.trim();
        catalogVersion = catalogVersion == null || catalogVersion.isBlank() ? "UNKNOWN" : catalogVersion.trim();
    }
}
