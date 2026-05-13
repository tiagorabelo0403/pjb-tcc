package com.tcc.pjb.backend.ai.financeira;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.IAPipelineContext;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class IAFinanceira implements IAService {

    private IAResponse ultimaResposta;

    @Override
    public String getTipo() {
        return "FINANCEIRA";
    }

    @Override
    public IAResponse getUltimaResposta() {
        return ultimaResposta;
    }

    @Override
    public IAResponse processar(IARequest request) {
        IAPipelineContext ctx = new IAPipelineContext(request);
        return processar(ctx);
    }

    @Override
    public IAResponse processar(IAPipelineContext context) {
        context.avancarEtapa("FINANCEIRA");

        IARequest req = context.getRequestEntrada();
        String tipoProcesso = req != null ? firstNonBlank(
                req.getSafeString("tipoProcesso"),
                req.getSafeString("tipo_processo"),
                req.getSafeString("ramo"),
                req.getSafeString("ramo_direito")
        ) : null;
        RamoDireito ramo = RamoDireito.fromString(tipoProcesso);

        String estimativa;
        double confianca;

        if (ramo == null) {
            estimativa = "Não foi possível inferir o ramo do Direito a partir do tipoProcesso informado. " +
                    "Sugestão: informar tipoProcesso (ex.: 'CIVIL', 'TRABALHISTA', 'TRIBUTARIO') e, se houver, valor da causa.";
            confianca = 0.42;
        } else {
            
            switch (ramo) {
                case TRABALHISTA -> {
                    estimativa = "Cenário trabalhista: considerar risco de sucumbência (CPC/CLT), " +
                            "possibilidade de acordo e custos de liquidação. " +
                            "Recomendável provisionar um intervalo e preparar prova documental e testemunhal.";
                    confianca = 0.75;
                }
                case TRIBUTARIO -> {
                    estimativa = "Cenário tributário: custos variam com complexidade do lançamento, " +
                            "probabilidade de tutela e impacto de depósito/garantia. " +
                            "Atenção a prazos decadenciais/prescricionais e risco de multa/juros.";
                    confianca = 0.74;
                }
                case ADMINISTRATIVO -> {
                    estimativa = "Cenário administrativo: avaliar necessidade de fase prévia (recurso/instância), " +
                            "custos de perícia e probabilidade de tutela de urgência.";
                    confianca = 0.70;
                }
                default -> {
                    estimativa = "Cenário geral: estimar custas iniciais, despesas de diligências, perícia (se aplicável) e risco de sucumbência. " +
                            "Para refinar, informe valor da causa, foro/competência e fase processual.";
                    confianca = 0.68;
                }
            }
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("pipeline_stage", context.getEtapaAtual());
        meta.put("ramo_direito", ramo != null ? ramo.name() : null);
        meta.put("timestamp", Instant.now().toString());

        IAResponse resp = IAResponse.builder()
                .origem(getTipo())
                .status(IAResponse.StatusIA.SUCESSO)
                .confianca(confianca)
                .texto(estimativa)
                .metadados(meta)
                .dataGeracao(Instant.now())
                .build();

        this.ultimaResposta = resp;
        context.setUltimaResposta(resp);
        context.memorizar("financeira_executada", true);
        return resp;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

}
