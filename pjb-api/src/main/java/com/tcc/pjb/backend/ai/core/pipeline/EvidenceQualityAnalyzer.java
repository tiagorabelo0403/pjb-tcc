package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

public final class EvidenceQualityAnalyzer {

    public EvidenceQualityReport analyze(List<EvidenceItem> evidences, ApiVersion version) {
        List<EvidenceItem> ev = (evidences == null) ? List.of() : evidences;
        ApiVersion v = (version != null) ? version : ApiVersion.latest();

        int count = ev.size();
        int diversity = estimateSourceDiversity(ev);
        double mean = meanScore(ev);

        int minE = minEvidence(v);
        int minD = minDiversity(v);

        boolean conflictRisk = conflictRiskByDispersion(ev, v);
        double suff = sufficiencyScore(count, diversity, mean, minE, minD, v);

        List<String> hints = new ArrayList<>();
        if (count < minE) {
            hints.add("Evidências insuficientes para a versão solicitada; ampliar busca RAG.");
        }
        if (diversity < minD) {
            hints.add("Baixa diversidade de fontes; incluir diretrizes/ensaios/revisões quando possível.");
        }
        if (mean > 0.0 && mean < 0.15 && v.isAtLeast(ApiVersion.V3)) {
            hints.add("Relevância média baixa; revisar termos de busca e filtros (capability/ramo).");
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("minEvidence", minE);
        meta.put("minDiversity", minD);
        meta.put("conflictRisk", conflictRisk);

        return new EvidenceQualityReport(count, diversity, mean, suff, conflictRisk, hints, meta);
    }

    private static int estimateSourceDiversity(List<EvidenceItem> ev) {
        if (ev == null || ev.isEmpty()) return 0;
        Set<String> src = new LinkedHashSet<>();
        for (EvidenceItem e : ev) {
            if (e == null) continue;
            String s = e.getFonteSistema();
            if (s == null || s.isBlank()) s = String.valueOf(e.getTipo());
            src.add(s.trim().toLowerCase(Locale.ROOT));
        }
        return src.size();
    }

    private static double meanScore(List<EvidenceItem> ev) {
        if (ev == null || ev.isEmpty()) return 0.0;
        double sum = 0.0;
        int n = 0;
        for (EvidenceItem e : ev) {
            if (e == null) continue;
            Double s = e.getScore();
            if (s == null) continue;
            sum += s;
            n++;
        }
        if (n <= 0) return 0.0;
        return sum / n;
    }

    private static int minEvidence(ApiVersion v) {
        Objects.requireNonNull(v, "v");
        if (v.isAtLeast(ApiVersion.V3)) return 3;
        if (v.isAtLeast(ApiVersion.V2)) return 2;
        return 1;
    }

    private static int minDiversity(ApiVersion v) {
        Objects.requireNonNull(v, "v");
        if (v.isAtLeast(ApiVersion.V3)) return 3;
        if (v.isAtLeast(ApiVersion.V2)) return 2;
        return 1;
    }

    private static double sufficiencyScore(int count, int diversity, double meanScore, int minE, int minD, ApiVersion v) {
        
        double ec = Math.min(1.0, (minE <= 0) ? 1.0 : ((double) count) / (double) minE);
        double dv = Math.min(1.0, (minD <= 0) ? 1.0 : ((double) diversity) / (double) minD);
        
        double ms = (meanScore <= 0.0) ? 0.0 : Math.min(1.0, meanScore);

        double base = (ec * 0.55) + (dv * 0.35) + (ms * 0.10);
        if (v.isAtLeast(ApiVersion.V3)) {
            
            base = base * 0.92;
        }
        return clamp01(base);
    }

    private static boolean conflictRiskByDispersion(List<EvidenceItem> ev, ApiVersion v) {
        
        
        if (ev == null || ev.size() < 4) return false;
        if (!v.isAtLeast(ApiVersion.V2)) return false;

        double mean = meanScore(ev);
        double var = 0.0;
        int n = 0;
        for (EvidenceItem e : ev) {
            if (e == null || e.getScore() == null) continue;
            double d = e.getScore() - mean;
            var += d * d;
            n++;
        }
        if (n <= 1) return false;
        double std = Math.sqrt(var / (double) n);

        int diversity = estimateSourceDiversity(ev);
        return (diversity >= minDiversity(v)) && (std < 0.08);
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
