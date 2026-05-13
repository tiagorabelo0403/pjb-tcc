package com.tcc.pjb.backend.ai.financeira.v1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.IAPipelineContext;
import com.tcc.pjb.backend.ai.core.IAService;
import com.tcc.pjb.backend.financial.ai.FinancialAiResponseFactory;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IAFinanceiraV1 implements IAService {

    private final FinancialAiResponseFactory responseFactory;
    private IAResponse ultimaResposta;

    @Override
    public String getTipo() {
        return "FINANCEIRA_V1";
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
        context.avancarEtapa("FINANCEIRA_V1");

        IARequest req = context.getRequestEntrada();
        var ramo = inferRamo(req);

        BigDecimal valorCausa = inferValorCausa(req);
        var estimate = FinancialEstimator.estimate(ramo, valorCausa);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("versao", 1);
        meta.put("ramo_direito", ramo != null ? ramo.name() : null);
        meta.put("valor_causa", valorCausa);
        meta.put("custas_range", Map.of("min", estimate.custasMin(), "max", estimate.custasMax()));
        meta.put("provisao_range", Map.of("min", estimate.provisaoMin(), "max", estimate.provisaoMax()));
        meta.put("risco_sucumbencia", estimate.riscoSucumbencia());
        meta.put("timestamp", Instant.now().toString());

        IAResponse base = IAResponse.builder()
                .origem(getTipo())
                .status(estimate.confidence() >= 0.60 ? IAResponse.StatusIA.SUCESSO : IAResponse.StatusIA.ALERTA)
                .confianca(estimate.confidence())
                .texto(estimate.summary())
                .metadados(meta)
                .dataGeracao(Instant.now())
                .build();
        IAResponse resp = base.adicionarMetadados(responseFactory.envelope(req, base, ApiVersion.V1));

        this.ultimaResposta = resp;
        context.setUltimaResposta(resp);
        context.memorizar("financeira_v1_executada", true);
        return resp;
    }


    private static RamoDireito inferRamo(IARequest req) {
        if (req == null) return null;
        String tipoProcesso = firstNonBlank(
                req.getSafeString("tipoProcesso"),
                req.getSafeString("tipo_processo"),
                req.getSafeString("ramo"),
                req.getSafeString("ramo_direito"),
                req.getSafeString("ramoDireito")
        );
        return RamoDireito.fromString(tipoProcesso);
    }

    private static BigDecimal inferValorCausa(IARequest req) {
        if (req == null) return null;

        Double v = firstNonNull(
                req.getSafeDouble("valorCausa"),
                req.getSafeDouble("valor_causa"),
                req.getSafeDouble("valorDaCausa"),
                req.getSafeDouble("valor_da_causa"),
                req.getSafeDouble("valor")
        );

        if (v == null || !Double.isFinite(v) || v <= 0.0) return null;
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) return null;
        for (T v : values) {
            if (v != null) return v;
        }
        return null;
    }

    static final class FinancialEstimator {

        static FinancialEstimate estimate(RamoDireito ramo, BigDecimal valorCausa) {
            var vc = Optional.ofNullable(valorCausa).filter(v -> v.compareTo(BigDecimal.ZERO) > 0);
            var ramoLabel = ramo != null ? ramo.name() : "INDEFINIDO";

            BigDecimal base = vc.orElse(BigDecimal.valueOf(10_000));
            BigDecimal custasMin = pct(base, 0.005).max(BigDecimal.valueOf(120));
            BigDecimal custasMax = pct(base, 0.015).min(BigDecimal.valueOf(25_000));

            BigDecimal overhead = switch (ramo != null ? ramo : RamoDireito.CIVIL) {
                case TRIBUTARIO -> BigDecimal.valueOf(5_000);
                case TRABALHISTA -> BigDecimal.valueOf(2_500);
                case ADMINISTRATIVO -> BigDecimal.valueOf(2_000);
                default -> BigDecimal.valueOf(1_500);
            };
            BigDecimal suc = pct(base, riscoSucumbencia(ramo));
            BigDecimal provisaoMin = custasMin.add(overhead).add(pct(suc, 0.25));
            BigDecimal provisaoMax = custasMax.add(overhead.multiply(BigDecimal.valueOf(2))).add(pct(suc, 0.80));

            double confidence = ramo != null ? (vc.isPresent() ? 0.78 : 0.66) : 0.52;

            String summary = buildNarrative(ramoLabel, valorCausa, custasMin, custasMax, provisaoMin, provisaoMax);

            return new FinancialEstimate(
                    custasMin,
                    custasMax,
                    provisaoMin,
                    provisaoMax,
                    riscoSucumbencia(ramo),
                    confidence,
                    summary
            );
        }

        private static BigDecimal pct(BigDecimal base, double p) {
            return base.multiply(BigDecimal.valueOf(p)).setScale(2, RoundingMode.HALF_UP);
        }

        private static double riscoSucumbencia(RamoDireito ramo) {
            return switch (ramo != null ? ramo : RamoDireito.CIVIL) {
                case TRABALHISTA -> 0.10;
                case TRIBUTARIO -> 0.15;
                case ADMINISTRATIVO -> 0.12;
                default -> 0.08;
            };
        }

        private static String buildNarrative(
                String ramo,
                BigDecimal valorCausa,
                BigDecimal custasMin,
                BigDecimal custasMax,
                BigDecimal provMin,
                BigDecimal provMax
        ) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Financeira V1] Estimativa determinística (ranges) — ramo=").append(ramo).append(".\n");

            if (valorCausa != null) {
                sb.append("Valor da causa informado: ").append(formatBRL(valorCausa)).append(".\n");
            } else {
                sb.append("Valor da causa não informado: usando base conservadora para ranges.\n");
            }

            sb.append("Custas (heurístico): ")
                    .append(formatBRL(custasMin))
                    .append(" — ")
                    .append(formatBRL(custasMax))
                    .append(".\n");

            sb.append("Provisão (custas + despesas + margem sucumbência): ")
                    .append(formatBRL(provMin))
                    .append(" — ")
                    .append(formatBRL(provMax))
                    .append(".\n\n");

            sb.append("Para refinar: informe foro/competência, fase processual, complexidade (perícia/contábil), ")
                    .append("e se há chance real de tutela/depósito/garantia.\n")
                    .append("Revisão humana recomendada.");

            return sb.toString();
        }

        private static String formatBRL(BigDecimal v) {
            Objects.requireNonNull(v);
            var s = v.setScale(2, RoundingMode.HALF_UP).toPlainString();
            return "R$ " + s.replace('.', ',');
        }
    }

    record FinancialEstimate(
            BigDecimal custasMin,
            BigDecimal custasMax,
            BigDecimal provisaoMin,
            BigDecimal provisaoMax,
            double riscoSucumbencia,
            double confidence,
            String summary
    ) {
        FinancialEstimate {
            Objects.requireNonNull(custasMin);
            Objects.requireNonNull(custasMax);
            Objects.requireNonNull(provisaoMin);
            Objects.requireNonNull(provisaoMax);
            Objects.requireNonNull(summary);
        }
    }
}
