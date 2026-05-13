package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import com.tcc.pjb.backend.platform.observability.ai.AiTelemetryDomain;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import lombok.RequiredArgsConstructor;

@RefreshScope
@Component
@RequiredArgsConstructor
public class HeuristicEvidenceContradictionResolver implements EvidenceContradictionResolver {

    private final AiContradictionResolverProperties props;

    @Override
    public EvidenceContradictionResolution resolve(List<EvidenceItem> evidences,
                                                   AiTelemetryDomain domain,
                                                   ApiVersion version,
                                                   EvidenceContradictionReport report) {
        AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
        ApiVersion v = (version != null) ? version : ApiVersion.latest();

        double raw = (report != null) ? clamp01(report.inconsistencyScore()) : 0.0;
        double proceedThreshold = thresholdFor(d, v);

        if (raw < proceedThreshold) {
            return EvidenceContradictionResolution.passthrough(report);
        }

        List<EvidenceItem> ev = (evidences == null) ? List.of() : evidences;

        if (report != null && report.mixedJurisdiction() && (d == AiTelemetryDomain.LEGAL)) {
            return EvidenceContradictionResolution.deny(
                    "mixed_jurisdiction",
                    raw,
                    List.of(
                            "Informe o tribunal/UF e o órgão julgador (se aplicável).",
                            "Informe a matéria/assunto e o rito/procedimento (se houver).",
                            "Confirme o recorte temporal (ano) e o tipo de decisão (monocrática/colegiada)."
                    ),
                    Map.of("proceedThreshold", proceedThreshold)
            );
        }

        EvidenceItem best = pickBestByTierAndScore(d, ev);
        if (best == null) {
            return EvidenceContradictionResolution.deny(
                    "no_anchor_evidence",
                    raw,
                    genericClarifications(d),
                    Map.of("proceedThreshold", proceedThreshold)
            );
        }

        int yearsHigh = temporalSpreadHighYears(d);
        boolean temporalSpreadHigh = report != null && report.temporalSpreadYears() >= yearsHigh;
        boolean mixedStance = report != null && (report.positiveStance() > 0 && report.negativeStance() > 0);

        int bestTier = tier(d, best.getTipo());
        double bestScore = (best.getScore() != null) ? best.getScore() : 0.0;

        double secondScore = secondBestScoreExcluding(ev, best);
        boolean strongAnchor = bestTier >= 3 && (bestScore - secondScore) >= 0.15;

        if (strongAnchor) {
            double residual = clamp01(Math.max(proceedThreshold - 0.03, raw * 0.85));

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("proceedThreshold", proceedThreshold);
            meta.put("anchorDocId", safe(best.getDocId()));
            meta.put("anchorType", best.getTipo() != null ? best.getTipo().name() : "UNKNOWN");
            meta.put("anchorTier", bestTier);
            meta.put("anchorScore", bestScore);
            meta.put("temporalSpreadHigh", temporalSpreadHigh);
            meta.put("mixedStance", mixedStance);

            String rationale = buildRationale(d, temporalSpreadHigh, mixedStance, best);

            return EvidenceContradictionResolution.allow(rationale, residual, meta);
        }

        List<String> clarifications = new ArrayList<>(genericClarifications(d));
        if (temporalSpreadHigh) {
            clarifications.add("Delimite o recorte temporal (ex.: últimos 3-5 anos) para reduzir conflito de época.");
        }
        if (mixedStance) {
            clarifications.add("Informe contexto/objetivo e critérios (norma aplicável, período, órgão) para escolher fontes comparáveis.");
        }

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("proceedThreshold", proceedThreshold);
        meta.put("anchorType", best.getTipo() != null ? best.getTipo().name() : "UNKNOWN");
        meta.put("anchorTier", bestTier);
        meta.put("anchorScore", bestScore);

        return EvidenceContradictionResolution.deny("insufficient_resolution", raw, clarifications, meta);
    }

    private double thresholdFor(AiTelemetryDomain d, ApiVersion v) {
        AiContradictionResolverProperties.DomainRules r = props.getDomains().get(d.tag());
        if (r == null) {
            return v.isAtLeast(ApiVersion.V3) ? 0.62 : v.isAtLeast(ApiVersion.V2) ? 0.58 : 0.55;
        }
        if (v.isAtLeast(ApiVersion.V3)) return r.getProceedThresholdV3();
        if (v.isAtLeast(ApiVersion.V2)) return r.getProceedThresholdV2();
        return r.getProceedThresholdV1();
    }

    private int temporalSpreadHighYears(AiTelemetryDomain d) {
        AiContradictionResolverProperties.DomainRules r = props.getDomains().get(d.tag());
        return (r != null) ? r.getTemporalSpreadYearsHigh() : 7;
    }

    private static EvidenceItem pickBestByTierAndScore(AiTelemetryDomain d, List<EvidenceItem> ev) {
        return ev.stream()
                .filter(e -> e != null)
                .max(Comparator
                        .comparingInt((EvidenceItem e) -> tier(d, e.getTipo()))
                        .thenComparingDouble(e -> e.getScore() != null ? e.getScore() : 0.0)
                )
                .orElse(null);
    }

    private static double secondBestScoreExcluding(List<EvidenceItem> ev, EvidenceItem best) {
        double second = 0.0;
        for (EvidenceItem e : ev) {
            if (e == null) continue;
            if (best != null && safe(best.getDocId()).equals(safe(e.getDocId()))) continue;
            double s = (e.getScore() != null) ? e.getScore() : 0.0;
            if (s > second) second = s;
        }
        return second;
    }

    private static int tier(AiTelemetryDomain d, EvidenceItem.EvidenceType type) {
        if (type == null) return 0;
        return switch (d) {
            case LEGAL -> switch (type) {
                case LEGISLACAO -> 4;
                case JURISPRUDENCIA -> 3;
                case DOUTRINA -> 2;
                case PECA_PROCESSUAL, DIARIO_OFICIAL -> 1;
                default -> 0;
            };
            case FINANCE -> switch (type) {
                case REGULATORY -> 4;
                case MARKET_DATA -> 2;
                default -> 0;
            };
        };
    }

    private static String buildRationale(AiTelemetryDomain d, boolean temporal, boolean stance, EvidenceItem anchor) {
        String cap = d.tag();
        String base = "resolved_by_anchor_tier";
        String extra = "";
        if (temporal) extra += "|temporal_preference";
        if (stance) extra += "|stance_mixed";
        String type = anchor != null && anchor.getTipo() != null ? anchor.getTipo().name() : "UNKNOWN";
        return (base + extra + "|domain=" + cap + "|anchor=" + type).toLowerCase(Locale.ROOT);
    }

    private static List<String> genericClarifications(AiTelemetryDomain d) {
        return switch (d) {
            case LEGAL -> List.of(
                    "Informe matéria/assunto, tribunal/UF e datas relevantes do caso.",
                    "Informe a peça/ato processual que originou a dúvida (intimação, sentença, acórdão).",
                    "Informe se há segredo de justiça e quais documentos podem ser considerados."
            );
            case FINANCE -> List.of(
                    "Informe valores, período de referência e hipótese de cálculo.",
                    "Informe base documental/regulatória aplicável (norma, resolução, tabela).",
                    "Informe se há indexadores/juros/correção e qual regime (simples/composto)."
            );
        };
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }
}
