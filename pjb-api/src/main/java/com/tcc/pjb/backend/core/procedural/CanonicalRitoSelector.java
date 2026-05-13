package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateResult;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CanonicalRitoSelector {

    public record SelectedRito(
            Instant selectedAt,
            String source,
            CanonicalContext canonicalContext,
            GateResult sanityGate,
            RitoProcessual rito,
            String status,
            boolean heuristicUsed,
            boolean fallbackApplied,
            Map<String, Object> metadata
    ) {
        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("selectedAt", selectedAt != null ? selectedAt.toString() : "");
            out.put("source", source == null || source.isBlank() ? "unspecified" : source);
            out.put("rito", rito != null ? rito.name() : "");
            out.put("status", status == null ? "UNRESOLVED" : status);
            out.put("heuristicUsed", heuristicUsed);
            out.put("fallbackApplied", fallbackApplied);
            out.put("canonicalContext", canonicalContext != null ? canonicalContext.toMap() : Map.of());
            out.put("sanityGate", sanityGate != null ? sanityGate.toMap() : Map.of());
            out.put("metadata", freezeMetadata(metadata));
            return freezeMetadata(out);
        }
    }

    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CanonicalSanityGate canonicalSanityGate;

    public CanonicalRitoSelector(ProceduralCanonicalResolver proceduralCanonicalResolver,
                                 CanonicalSanityGate canonicalSanityGate) {
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.canonicalSanityGate = Objects.requireNonNull(canonicalSanityGate);
    }

    public SelectedRito select(Map<String, Object> payload, String heuristicRitoRaw, String source) {
        return select(payload, normalizeHeuristic(heuristicRitoRaw), source);
    }

    public SelectedRito select(Map<String, Object> payload, RitoProcessual heuristicRito, String source) {
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(payload == null ? Map.of() : payload);
        GateResult gateResult = canonicalSanityGate.evaluate(canonicalContext);
        RitoProcessual effectiveRito = canonicalContext.rito();
        String status = gateResult.overallStatus();
        boolean heuristicUsed = false;
        boolean fallbackApplied = false;
        if (effectiveRito == null && heuristicRito != null) {
            effectiveRito = heuristicRito;
            heuristicUsed = true;
            status = gateResult.hasBlockingIssues() ? gateResult.overallStatus() : "HEURISTIC_COMPATIBILITY";
        }
        if (effectiveRito == null) {
            effectiveRito = RitoProcessual.COMUM_ORDINARIO;
            fallbackApplied = true;
            status = gateResult.hasBlockingIssues() ? gateResult.overallStatus() : "LEGACY_DEFAULT_APPLIED";
        }
        if (canonicalContext.rito() != null && !gateResult.hasBlockingIssues()) {
            status = "CANONICAL_RITO_RESOLVED";
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("canonicalRito", canonicalContext.rito() != null ? canonicalContext.rito().name() : null);
        metadata.put("heuristicRito", heuristicRito != null ? heuristicRito.name() : null);
        metadata.put("effectiveRito", effectiveRito.name());
        metadata.put("sanityStatus", gateResult.overallStatus());
        metadata.put("statusCodes", gateResult.statusCodes());
        return new SelectedRito(
                Instant.now(),
                normalizeSource(source),
                canonicalContext,
                gateResult,
                effectiveRito,
                status,
                heuristicUsed,
                fallbackApplied,
                freezeMetadata(metadata)
        );
    }


    private static Map<String, Object> freezeMetadata(Map<String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : metadata.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                frozen.put(entry.getKey(), entry.getValue());
            }
        }
        return frozen.isEmpty() ? Map.of() : Map.copyOf(frozen);
    }

    private static RitoProcessual normalizeHeuristic(String heuristicRitoRaw) {
        if (heuristicRitoRaw == null || heuristicRitoRaw.isBlank()) {
            return null;
        }
        return RitoProcessual.tryParse(heuristicRitoRaw).orElse(null);
    }

    private static String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "unspecified";
        }
        return source.trim();
    }
}
