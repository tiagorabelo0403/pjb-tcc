package com.tcc.pjb.backend.ai.financeira.v3;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.IAPipelineContext;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.ai.financeira.v2.IAFinanceiraV2;
import com.tcc.pjb.backend.financial.ai.FinancialAiResponseFactory;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IAFinanceiraV3 implements IAService {

    private final IAFinanceiraV2 v2;
    private final FinancialExplainabilityEngine explainability;
    private final FinancialAiResponseFactory responseFactory;
    private IAResponse ultimaResposta;

    @Override
    public String getTipo() {
        return "FINANCEIRA_V3";
    }

    @Override
    public IAResponse getUltimaResposta() {
        return ultimaResposta;
    }

    @Override
    public IAResponse processar(IARequest request) {
        return processar(new IAPipelineContext(request));
    }

    @Override
    public IAResponse processar(IAPipelineContext context) {
        context.avancarEtapa("FINANCEIRA_V3");

        IAResponse base = v2.processar(context);
        IARequest req = context.getRequestEntrada();

        var expl = explainability.explain(req, base);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("versao", 3);
        meta.put("base_origem", base != null ? base.getOrigem() : null);
        meta.put("explainability", expl);
        meta.put("etapas", context.getStageHistory());

        String texto = (base != null && base.getTexto() != null ? base.getTexto() : "")
                + "\n\n[V3 - Explainability]\n"
                + expl.humanReadable();

        IAResponse merged = (base != null ? base.toBuilder() : IAResponse.builder())
                .origem(getTipo())
                .texto(texto)
                .metadados(merge(base != null ? base.getMetadados() : null, meta))
                .dataGeracao(Instant.now())
                .build();

        IAResponse resp = merged.adicionarMetadados(responseFactory.envelope(req, merged, ApiVersion.V3));

        this.ultimaResposta = resp;
        context.setUltimaResposta(resp);
        context.memorizar("financeira_v3_executada", true);
        return resp;
    }

    private static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (a != null) out.putAll(a);
        if (b != null) out.putAll(b);
        return out;
    }
}
