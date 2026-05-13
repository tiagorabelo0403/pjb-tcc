package com.tcc.pjb.backend.ai.agentic.agents.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.agentic.agents.common.Agent;
import com.tcc.pjb.backend.ai.agentic.core.AgentResult;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunRequest;

@Component
public class CashFlowStressAgent implements Agent {

    @Override
    public String name() {
        return "CashFlowStressAgent";
    }

    @Override
    public AgentResult execute(AgenticRunRequest request) {
        Map<String, Object> in = request != null ? request.getInput() : Map.of();
        CashFlowSnapshot snap = CashFlowSnapshot.from(in);

        Map<String, Object> stress = stressTest(snap);
        double confidence = snap.coverageScore();

        List<String> actions = new ArrayList<>();
        if (confidence < 0.55) actions.add("fornecer_fluxo_de_caixa_e_saldo" );
        actions.add("validar_horizonte_e_premissas" );

        AgentResult out = new AgentResult();
        out.setAgent(name());
        out.setConfidence(confidence);
        out.setHumanReviewRequired(confidence < 0.45);
        out.setData(Map.of(
                "snapshot", snap.toMap(),
                "stress", stress,
                "actions", actions
        ));
        return out;
    }

    private static Map<String, Object> stressTest(CashFlowSnapshot s) {
        Map<String, Object> out = new LinkedHashMap<>();

        BigDecimal monthlyBurn = s.monthlyBurn;
        BigDecimal cash = s.cash;

        BigDecimal runwayMonths = null;
        if (monthlyBurn.signum() > 0) {
            runwayMonths = cash.divide(monthlyBurn, 2, RoundingMode.HALF_UP);
        }

        BigDecimal fcfMargin = div(s.freeCashFlow, s.revenue);

        out.put("runwayMonths", runwayMonths);
        out.put("freeCashFlow", s.freeCashFlow);
        out.put("freeCashFlowMargin", fcfMargin);

        BigDecimal stressBurn = monthlyBurn.multiply(new BigDecimal("1.25"));
        BigDecimal stressedRunway = stressBurn.signum() > 0 ? cash.divide(stressBurn, 2, RoundingMode.HALF_UP) : null;
        out.put("stressedRunwayMonths", stressedRunway);

        return out;
    }

    private static BigDecimal div(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        if (b.signum() == 0) return null;
        return a.divide(b, 6, RoundingMode.HALF_UP);
    }

    private record CashFlowSnapshot(BigDecimal revenue, BigDecimal cash, BigDecimal freeCashFlow, BigDecimal monthlyBurn, int coverage) {

        static CashFlowSnapshot from(Map<String, Object> in) {
            Map<String, Object> cf = map(in.get("cashFlow"));
            Map<String, Object> bs = map(in.get("balanceSheet"));
            Map<String, Object> dre = map(in.get("dre"));

            BigDecimal revenue = num(firstNonNull(dre, "revenue", "receita", "receitaLiquida"));
            BigDecimal cash = num(firstNonNull(bs, "cash", "caixa", "cashAndEquivalents"));

            BigDecimal operating = num(firstNonNull(cf, "operatingCashFlow", "fco"));
            BigDecimal capex = num(firstNonNull(cf, "capex", "investimentos"));
            BigDecimal free = num(firstNonNull(cf, "freeCashFlow", "fcf"));
            if (free == null && operating != null) {
                free = operating.subtract(capex == null ? BigDecimal.ZERO : capex);
            }

            BigDecimal monthlyBurn = num(firstNonNull(cf, "monthlyBurn", "burn"));
            if (monthlyBurn == null && free != null && free.signum() < 0) {
                monthlyBurn = free.abs().divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
            }

            int cov = 0;
            cov += revenue != null ? 1 : 0;
            cov += cash != null ? 1 : 0;
            cov += free != null ? 1 : 0;
            cov += monthlyBurn != null ? 1 : 0;

            return new CashFlowSnapshot(
                    nz(revenue), nz(cash), nz(free), nz(monthlyBurn), cov
            );
        }

        double coverageScore() {
            return Math.max(0.15, Math.min(0.95, 0.30 + 0.65 * (coverage / 4.0)));
        }

        Map<String, Object> toMap() {
            return Map.of(
                    "revenue", revenue,
                    "cash", cash,
                    "freeCashFlow", freeCashFlow,
                    "monthlyBurn", monthlyBurn
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
