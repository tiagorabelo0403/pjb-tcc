package com.tcc.pjb.backend.ai.financeira.v2;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.IAPipelineContext;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.ai.financeira.v1.IAFinanceiraV1;
import com.tcc.pjb.backend.financial.ai.FinancialAiResponseFactory;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IAFinanceiraV2 implements IAService {

    private final IAFinanceiraV1 v1;
    private final FinancialAiResponseFactory responseFactory;
    private IAResponse ultimaResposta;

    @Override
    public String getTipo() {
        return "FINANCEIRA_V2";
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
        context.avancarEtapa("FINANCEIRA_V2");

        IAResponse base = v1.processar(context);
        IARequest req = context.getRequestEntrada();

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("versao", 2);
        meta.put("base_origem", base != null ? base.getOrigem() : null);
        meta.put("etapas", context.getStageHistory());
        meta.put("risk_matrix", FinancialRiskMatrix.from(req));
        meta.put("missing_inputs", FinancialRiskMatrix.missingInputs(req));

        String texto = (base != null && base.getTexto() != null ? base.getTexto() : "")
                + "\n\n[V2 - Risco/Impacto]\n"
                + FinancialRiskMatrix.humanReadable(req);

        IAResponse merged = (base != null ? base.toBuilder() : IAResponse.builder())
                .origem(getTipo())
                .texto(texto)
                .metadados(merge(base != null ? base.getMetadados() : null, meta))
                .dataGeracao(Instant.now())
                .build();

        IAResponse resp = merged.adicionarMetadados(responseFactory.envelope(req, merged, ApiVersion.V2));

        this.ultimaResposta = resp;
        context.setUltimaResposta(resp);
        context.memorizar("financeira_v2_executada", true);
        return resp;
    }

    private static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (a != null) out.putAll(a);
        if (b != null) out.putAll(b);
        return out;
    }
}
