package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.*;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;

public final class EvidenceNormalizer {

    private EvidenceNormalizer() {}

    public static List<EvidenceItem> dedupeByDocIdKeepBestScore(List<EvidenceItem> in, int maxItems) {
        if (in == null || in.isEmpty()) return List.of();
        int max = maxItems <= 0 ? 50 : maxItems;

        Map<String, EvidenceItem> best = new LinkedHashMap<>();
        for (EvidenceItem e : in) {
            if (e == null) continue;
            String id = safeKey(e.getDocId());
            if (id == null) {
                
                id = safeKey(e.getTitulo()) + "|" + safeKey(e.getTrecho());
            }
            EvidenceItem prev = best.get(id);
            if (prev == null) {
                best.put(id, e);
                continue;
            }
            Double p = prev.getScore();
            Double n = e.getScore();
            
            if (n != null && (p == null || n > p)) {
                best.put(id, e);
            }
        }

        List<EvidenceItem> out = new ArrayList<>(best.values());
        out.sort((a, b) -> {
            Double sa = a != null ? a.getScore() : null;
            Double sb = b != null ? b.getScore() : null;
            if (sa == null && sb == null) return 0;
            if (sa == null) return 1;
            if (sb == null) return -1;
            return Double.compare(sb, sa);
        });
        if (out.size() > max) out = out.subList(0, max);
        return List.copyOf(out);
    }

    private static String safeKey(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        if (t.length() > 120) t = t.substring(0, 120);
        return t;
    }
}
