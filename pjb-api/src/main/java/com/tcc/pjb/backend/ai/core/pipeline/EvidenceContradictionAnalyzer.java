package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import com.tcc.pjb.backend.platform.observability.ai.AiTelemetryDomain;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

public final class EvidenceContradictionAnalyzer {

    private static final Pattern YEAR = Pattern.compile("(19[5-9]\\d|20\\d\\d)");

    public EvidenceContradictionReport analyze(List<EvidenceItem> evidences,
                                               AiTelemetryDomain domain,
                                               ApiVersion version) {
        List<EvidenceItem> ev = (evidences == null) ? List.of() : evidences;
        AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
        ApiVersion v = (version != null) ? version : ApiVersion.latest();

        int pos = 0;
        int neg = 0;
        int unc = 0;
        int unk = 0;

        int hasReg = 0;
        int hasMarket = 0;

        int minYear = Integer.MAX_VALUE;
        int maxYear = Integer.MIN_VALUE;

        Set<String> jurisOrSources = new LinkedHashSet<>();

        for (EvidenceItem e : ev) {
            if (e == null) continue;

            if (e.getTipo() != null) {
                switch (e.getTipo()) {
                    case REGULATORY -> hasReg++;
                    case MARKET_DATA -> hasMarket++;
                    default -> {
                    }
                }
            }

            String text = concatForHeuristics(e);
            Stance s = detectStance(text, d);
            switch (s) {
                case POSITIVE -> pos++;
                case NEGATIVE -> neg++;
                case UNCERTAIN -> unc++;
                default -> unk++;
            }

            int y = extractYear(text);
            if (y > 0) {
                if (y < minYear) minYear = y;
                if (y > maxYear) maxYear = y;
            }

            if (e.getTribunal() != null && !e.getTribunal().isBlank()) {
                String j = normalizeToken(e.getTribunal());
                if (j != null) jurisOrSources.add(j);
            }
            if (e.getFonteSistema() != null && !e.getFonteSistema().isBlank()) {
                String j = normalizeToken(e.getFonteSistema());
                if (j != null) jurisOrSources.add(j);
            }
        }

        int total = pos + neg + unc + unk;
        int temporalSpread = (minYear == Integer.MAX_VALUE || maxYear == Integer.MIN_VALUE) ? 0 : (maxYear - minYear);

        double contradiction = contradictionScore(pos, neg, total, d, v);
        boolean mixedJurisdiction = mixedJurisdiction(d, jurisOrSources);

        double inconsistency = inconsistencyScore(contradiction, temporalSpread, mixedJurisdiction, v);

        boolean marketOnly = (d == AiTelemetryDomain.FINANCE) && (hasMarket > 0) && (hasReg == 0) && v.isAtLeast(ApiVersion.V3);
        if (marketOnly) inconsistency = clamp01(inconsistency + 0.10);

        List<String> signals = new ArrayList<>();
        if (pos > 0 && neg > 0) signals.add("mixed_stance");
        if (temporalSpread >= (v.isAtLeast(ApiVersion.V3) ? 8 : 12)) signals.add("temporal_spread_high");
        if (mixedJurisdiction) signals.add("mixed_jurisdiction_or_sources");
        if (unc > 0) signals.add("inconclusive_language_present");
        if (marketOnly) signals.add("market_without_regulatory_basis");

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("total", total);
        meta.put("jurisdictionDistinct", jurisOrSources.size());
        meta.put("minYear", (minYear == Integer.MAX_VALUE) ? null : minYear);
        meta.put("maxYear", (maxYear == Integer.MIN_VALUE) ? null : maxYear);
        meta.put("typeCounts", Map.of(
                "regulatory", hasReg,
                "market", hasMarket
        ));

        return new EvidenceContradictionReport(
                contradiction,
                inconsistency,
                pos,
                neg,
                unc,
                unk,
                temporalSpread,
                mixedJurisdiction,
                signals,
                meta
        );
    }

    private static String concatForHeuristics(EvidenceItem e) {
        StringBuilder sb = new StringBuilder();
        if (e.getTitulo() != null) sb.append(e.getTitulo()).append(' ');
        if (e.getTrecho() != null) sb.append(e.getTrecho()).append(' ');
        if (e.getTribunal() != null) sb.append(e.getTribunal()).append(' ');
        if (e.getFonteSistema() != null) sb.append(e.getFonteSistema()).append(' ');
        return sb.toString();
    }

    private static int extractYear(String s) {
        if (s == null || s.isBlank()) return 0;
        Matcher m = YEAR.matcher(s);
        int best = 0;
        while (m.find()) {
            try {
                int y = Integer.parseInt(m.group(1));
                if (y >= 1950 && y <= 2099) best = Math.max(best, y);
            } catch (NumberFormatException ignored) {
            }
        }
        return best;
    }

    private static double contradictionScore(int pos, int neg, int total, AiTelemetryDomain d, ApiVersion v) {
        if (total <= 0) return 0.0;
        if (pos <= 0 || neg <= 0) return 0.0;

        double balance = ((double) Math.min(pos, neg)) / (double) Math.max(pos, neg);
        double density = ((double) (pos + neg)) / (double) total;
        double base = balance * density;

        if (d == AiTelemetryDomain.LEGAL) base *= 1.05;
        if (v != null && v.isAtLeast(ApiVersion.V3)) base *= 1.08;
        return clamp01(base);
    }

    private static boolean mixedJurisdiction(AiTelemetryDomain d, Set<String> tokens) {
        if (tokens == null || tokens.size() <= 1) return false;
        if (d != AiTelemetryDomain.LEGAL) return tokens.size() >= 3;

        boolean hasStf = containsPrefix(tokens, "stf");
        boolean hasStj = containsPrefix(tokens, "stj");
        boolean hasTrf = containsPrefix(tokens, "trf");
        boolean hasTj = containsPrefix(tokens, "tj");
        int levels = 0;
        if (hasStf) levels++;
        if (hasStj) levels++;
        if (hasTrf) levels++;
        if (hasTj) levels++;
        return levels >= 2;
    }

    private static boolean containsPrefix(Set<String> tokens, String prefix) {
        if (tokens == null || prefix == null) return false;
        for (String t : tokens) {
            if (t == null) continue;
            if (t.startsWith(prefix)) return true;
        }
        return false;
    }

    private static double inconsistencyScore(double contradiction, int temporalSpreadYears, boolean mixedJurisdiction, ApiVersion v) {
        Objects.requireNonNull(v, "v");
        double temporal = (temporalSpreadYears <= 0) ? 0.0 : clamp01(((double) temporalSpreadYears) / 15.0);
        double juris = mixedJurisdiction ? 0.35 : 0.0;
        double base = (contradiction * 0.55) + (temporal * 0.30) + juris;
        if (v.isAtLeast(ApiVersion.V3)) base *= 1.05;
        return clamp01(base);
    }

    private static String normalizeToken(String s) {
        if (s == null || s.isBlank()) return null;
        String out = s.trim().toLowerCase(Locale.ROOT);
        out = out.replaceAll("[^a-z0-9]", "");
        if (out.isBlank()) return null;
        return out;
    }

    private enum Stance { POSITIVE, NEGATIVE, UNCERTAIN, UNKNOWN }

    private static Stance detectStance(String text, AiTelemetryDomain d) {
        if (text == null || text.isBlank()) return Stance.UNKNOWN;
        String s = text.toLowerCase(Locale.ROOT);

        if (containsAny(s,
                "inconclusive", "inconclusivo", "insufficient evidence", "evidence insufficient",
                "não conclusivo", "nao conclusivo", "sem evidência suficiente", "sem evidencia suficiente")) {
            return Stance.UNCERTAIN;
        }

        if (d == AiTelemetryDomain.LEGAL) {
            if (containsAny(s, "indefer", "negou", "nega", "improced", "desprov", "rejeit", "nao conhecido", "não conhecido", "improvido")) {
                return Stance.NEGATIVE;
            }
            if (containsAny(s, "defer", "conced", "proced", "provimento", "acolh", "tutela conced")) {
                return Stance.POSITIVE;
            }
        } else {
            if (containsAny(s, "vedad", "proib", "irregular", "ilegal", "não permitido", "nao permitido", "nao autorizado", "não autorizado")) {
                return Stance.NEGATIVE;
            }
            if (containsAny(s, "autoriz", "permit", "regular", "aprovad", "compliance", "permitido")) {
                return Stance.POSITIVE;
            }
        }

        return Stance.UNKNOWN;
    }

    private static boolean containsAny(String s, String... needles) {
        if (s == null || s.isBlank() || needles == null) return false;
        for (String n : needles) {
            if (n == null || n.isBlank()) continue;
            if (s.contains(n)) return true;
        }
        return false;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
