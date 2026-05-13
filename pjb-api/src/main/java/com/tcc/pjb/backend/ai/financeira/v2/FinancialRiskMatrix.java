package com.tcc.pjb.backend.ai.financeira.v2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.tcc.pjb.backend.ai.contract.IARequest;

final class FinancialRiskMatrix {

    private FinancialRiskMatrix() {
    }

    static Map<String, Object> from(IARequest req) {
        double p = clamp01(inferProbability(req));
        double i = clamp01(inferImpact(req));
        String band = band(p * i);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("probability", p);
        m.put("impact", i);
        m.put("risk_score", round2(p * i));
        m.put("band", band);
        return m;
    }

    static List<String> missingInputs(IARequest req) {
        List<String> missing = new ArrayList<>();
        if (req == null) {
            missing.add("tipoProcesso/ramo_direito");
            missing.add("valorCausa");
            missing.add("faseProcessual");
            return missing;
        }
        if (blank(req.getSafeString("tipoProcesso"))
                && blank(req.getSafeString("ramo"))
                && blank(req.getSafeString("ramo_direito"))) {
            missing.add("tipoProcesso/ramo_direito");
        }
        if (req.getSafeDouble("valorCausa") == null
                && req.getSafeDouble("valor_causa") == null
                && req.getSafeDouble("valorDaCausa") == null
                && req.getSafeDouble("valor_da_causa") == null) {
            missing.add("valorCausa");
        }
        if (blank(req.getSafeString("faseProcessual")) && blank(req.getSafeString("fase"))) {
            missing.add("faseProcessual");
        }
        return missing;
    }

    static String humanReadable(IARequest req) {
        var m = from(req);
        String band = String.valueOf(m.get("band"));
        String score = String.valueOf(m.get("risk_score"));
        var missing = missingInputs(req);
        StringBuilder sb = new StringBuilder();
        sb.append("Risco estimado (P×I): ").append(score).append(" (banda: ").append(band).append(").\n");
        if (!missing.isEmpty()) {
            sb.append("Inputs faltantes para refino: ").append(String.join(", ", missing)).append(".\n");
        }
        sb.append("Sugestão: informe foro/competência, fase, valor e se há perícia/contábil.");
        return sb.toString();
    }

    private static double inferProbability(IARequest req) {
        if (req == null) return 0.55;
        Double p = req.getSafeDouble("probabilidade");
        if (p != null) return p;

        String fase = lower(firstNonBlank(req.getSafeString("faseProcessual"), req.getSafeString("fase")));
        if (fase.contains("tutela") || fase.contains("liminar")) return 0.50;
        if (fase.contains("senten") || fase.contains("recurso")) return 0.60;
        return 0.55;
    }

    private static double inferImpact(IARequest req) {
        if (req == null) return 0.55;
        Double i = req.getSafeDouble("impacto");
        if (i != null) return i;

        Double vc = firstNonNull(
                req.getSafeDouble("valorCausa"),
                req.getSafeDouble("valor_causa"),
                req.getSafeDouble("valorDaCausa"),
                req.getSafeDouble("valor_da_causa")
        );
        if (vc == null) return 0.55;
        if (vc >= 1_000_000) return 0.90;
        if (vc >= 250_000) return 0.75;
        if (vc >= 50_000) return 0.60;
        return 0.45;
    }

    private static String band(double score) {
        if (score >= 0.70) return "ALTO";
        if (score >= 0.45) return "MEDIO";
        return "BAIXO";
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return "";
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) return null;
        for (T v : values) {
            if (v != null) return v;
        }
        return null;
    }
}
