package com.tcc.pjb.backend.ai.agentic.agents.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.agentic.agents.common.Agent;
import com.tcc.pjb.backend.ai.agentic.core.AgentResult;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunRequest;

@Component
public class FinancialRatiosAgent implements Agent {

    @Override
    public String name() {
        return "FinancialRatiosAgent";
    }

    @Override
    public AgentResult execute(AgenticRunRequest request) {
        Map<String, Object> in = request != null ? request.getInput() : Map.of();
        FinancialSnapshot snap = FinancialSnapshot.from(in);

        Map<String, Object> ratios = computeRatios(snap);
        double confidence = snap.coverageScore();

        List<String> actions = new ArrayList<>();
        if (confidence < 0.55) {
            actions.add("fornecer_dre_balanco_e_caixa");
        }
        actions.add("validar_periodo_e_moeda");
        if (snap.totalDebt.signum() > 0) {
            actions.add("detalhar_composicao_divida");
        }

        AgentResult out = new AgentResult();
        out.setAgent(name());
        out.setConfidence(confidence);
        out.setHumanReviewRequired(confidence < 0.50);
        out.setData(Map.of(
                "snapshot", snap.toMap(),
                "ratios", ratios,
                "actions", actions
        ));
        return out;
    }

    private static Map<String, Object> computeRatios(FinancialSnapshot s) {
        Map<String, Object> out = new LinkedHashMap<>();

        put(out, "currentRatio", div(s.currentAssets, s.currentLiabilities));
        put(out, "quickRatio", div(s.quickAssets, s.currentLiabilities));
        put(out, "netDebt", s.totalDebt.subtract(s.cash));
        put(out, "debtToEquity", div(s.totalDebt, s.equity));
        put(out, "netMargin", div(s.netIncome, s.revenue));
        put(out, "ebitdaMargin", div(s.ebitda, s.revenue));
        put(out, "interestCoverage", div(s.ebitda, s.interestExpense));

        return out;
    }

    private static void put(Map<String, Object> out, String k, BigDecimal v) {
        if (v == null) return;
        out.put(k, v);
    }

    private static BigDecimal div(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        if (b.signum() == 0) return null;
        return a.divide(b, 6, RoundingMode.HALF_UP);
    }

    private record FinancialSnapshot(
            BigDecimal revenue,
            BigDecimal ebitda,
            BigDecimal netIncome,
            BigDecimal cash,
            BigDecimal totalDebt,
            BigDecimal equity,
            BigDecimal currentAssets,
            BigDecimal quickAssets,
            BigDecimal currentLiabilities,
            BigDecimal interestExpense,
            int coverage
    ) {
        static FinancialSnapshot from(Map<String, Object> in) {
            Map<String, Object> bs = map(in.get("balanceSheet"));
            Map<String, Object> dre = map(in.get("dre"));

            BigDecimal revenue = num(firstNonNull(dre, "revenue", "receita", "receitaLiquida"));
            BigDecimal ebitda = num(firstNonNull(dre, "ebitda"));
            BigDecimal netIncome = num(firstNonNull(dre, "netIncome", "lucroLiquido", "resultadoLiquido"));
            BigDecimal interest = num(firstNonNull(dre, "interestExpense", "juros"));

            BigDecimal cash = num(firstNonNull(bs, "cash", "caixa", "cashAndEquivalents"));
            BigDecimal debt = num(firstNonNull(bs, "totalDebt", "dividaBruta", "debt"));
            BigDecimal equity = num(firstNonNull(bs, "equity", "patrimonioLiquido", "pl"));

            BigDecimal currentAssets = num(firstNonNull(bs, "currentAssets", "ativoCirculante"));
            BigDecimal currentLiabilities = num(firstNonNull(bs, "currentLiabilities", "passivoCirculante"));
            BigDecimal quickAssets = num(firstNonNull(bs, "quickAssets", "ativoCirculanteLiquido"));
            if (quickAssets == null) quickAssets = currentAssets;

            int cov = 0;
            cov += revenue != null ? 1 : 0;
            cov += ebitda != null ? 1 : 0;
            cov += netIncome != null ? 1 : 0;
            cov += cash != null ? 1 : 0;
            cov += debt != null ? 1 : 0;
            cov += equity != null ? 1 : 0;
            cov += currentAssets != null ? 1 : 0;
            cov += currentLiabilities != null ? 1 : 0;

            return new FinancialSnapshot(
                    nz(revenue), nz(ebitda), nz(netIncome), nz(cash), nz(debt), nz(equity),
                    nz(currentAssets), nz(quickAssets), nz(currentLiabilities), nz(interest),
                    cov
            );
        }

        double coverageScore() {
            return Math.max(0.15, Math.min(0.95, 0.25 + 0.70 * (coverage / 8.0)));
        }

        Map<String, Object> toMap() {
            return Map.of(
                    "revenue", revenue,
                    "ebitda", ebitda,
                    "netIncome", netIncome,
                    "cash", cash,
                    "totalDebt", totalDebt,
                    "equity", equity,
                    "currentAssets", currentAssets,
                    "currentLiabilities", currentLiabilities,
                    "interestExpense", interestExpense
            );
        }

        private static BigDecimal nz(BigDecimal v) {
            return v == null ? BigDecimal.ZERO : v;
        }

        private static Object firstNonNull(Map<String, Object> m, String... keys) {
            if (m == null || m.isEmpty()) return null;
            for (String k : keys) {
                if (k == null) continue;
                Object v = m.get(k);
                if (v != null) return v;
            }
            return null;
        }

        private static Map<String, Object> map(Object o) {
            if (o instanceof Map<?, ?> mm) {
                Map<String, Object> out = new HashMap<>();
                for (Map.Entry<?, ?> e : mm.entrySet()) {
                    if (e.getKey() == null) continue;
                    out.put(String.valueOf(e.getKey()), e.getValue());
                }
                return out;
            }
            return Map.of();
        }

        private static BigDecimal num(Object o) {
            if (o == null) return null;
            if (o instanceof BigDecimal bd) return bd;
            if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
            String s = String.valueOf(o).trim();
            if (s.isBlank()) return null;
            s = s.replace(" ", "").replace(".", "").replace(",", ".");
            try {
                return new BigDecimal(s);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
