package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LegalEvidenceTrustClassifier {

    private static final Set<String> OFFICIAL_CODES = Set.of(
            "PJB",
            "PJE",
            "E-SAJ",
            "ESAJ",
            "EPROC",
            "CRETA",
            "PROJUDI",
            "SEEU",
            "CNJ",
            "DATAJUD",
            "MNI",
            "DJE",
            "STF",
            "STJ",
            "TST",
            "STM",
            "TSE",
            "TRF",
            "TJ",
            "TRE",
            "TRT"
    );
    private static final List<String> INSTITUTIONAL_MARKERS = List.of(
            "workspace",
            "gabinete",
            "secretaria",
            "institucional",
            "institutional",
            "procuradoria",
            "defensoria",
            "promotoria",
            "ministerio-publico",
            "ministerio_publico",
            "patrono",
            "office",
            "interno",
            "internal"
    );
    private static final List<String> DERIVED_MARKERS = List.of(
            "resumo",
            "sumario",
            "sintese",
            "summary",
            "ocr",
            "transcricao",
            "transcription",
            "anotacao",
            "notes",
            "nota",
            "consolidado",
            "merged",
            "derivado",
            "derived"
    );

    public EvidenceTrustDecision classify(LegalAiConversationRequest request,
                                          LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                          LegalAiConversationTrustZoneSnapshot trustZone) {
        LinkedHashSet<String> officialEvidenceIds = new LinkedHashSet<>(documentSecurity == null || documentSecurity.allowlistedSources() == null ? List.of() : documentSecurity.allowlistedSources());
        LinkedHashSet<String> institutionalControlledEvidenceIds = new LinkedHashSet<>();
        LinkedHashSet<String> derivedEvidenceIds = new LinkedHashSet<>();
        LinkedHashSet<String> untrustedEvidenceIds = new LinkedHashSet<>(documentSecurity == null || documentSecurity.blockedSources() == null ? List.of() : documentSecurity.blockedSources());
        collectCandidates(request == null ? null : request.context()).forEach(candidate -> classifyCandidate(candidate, officialEvidenceIds, institutionalControlledEvidenceIds, derivedEvidenceIds, untrustedEvidenceIds));
        String tier = resolveTier(officialEvidenceIds, institutionalControlledEvidenceIds, derivedEvidenceIds, untrustedEvidenceIds);
        List<String> reasons = new ArrayList<>();
        if (!officialEvidenceIds.isEmpty()) {
            reasons.add("A malha detectou fonte oficial apta a ancorar promoção soberana de evidência.");
        }
        if (!institutionalControlledEvidenceIds.isEmpty()) {
            reasons.add("A malha detectou fonte institucional controlada que exige fronteira soberana antes de promoção automática.");
        }
        if (!derivedEvidenceIds.isEmpty()) {
            reasons.add("A malha detectou fonte derivada e manteve promoção assistida antes de grounding ou RAG.");
        }
        if (!untrustedEvidenceIds.isEmpty()) {
            reasons.add("A malha detectou fonte não confiável e travou promoção soberana enquanto persistir contaminação externa.");
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("trustZone", trustZone == null ? null : trustZone.trustZone());
        diagnostics.put("trustZoneStatus", trustZone == null ? null : trustZone.status());
        diagnostics.put("sourceEvidenceTier", tier);
        diagnostics.put("officialSourceCount", officialEvidenceIds.size());
        diagnostics.put("institutionalSourceCount", institutionalControlledEvidenceIds.size());
        diagnostics.put("derivedSourceCount", derivedEvidenceIds.size());
        diagnostics.put("untrustedSourceCount", untrustedEvidenceIds.size());
        return new EvidenceTrustDecision(
                tier,
                List.copyOf(officialEvidenceIds),
                List.copyOf(institutionalControlledEvidenceIds),
                List.copyOf(derivedEvidenceIds),
                List.copyOf(untrustedEvidenceIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private LinkedHashSet<String> collectCandidates(Map<String, Object> context) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (context == null || context.isEmpty()) {
            return out;
        }
        context.forEach((key, value) -> {
            String normalizedKey = normalize(key);
            if (normalizedKey == null) {
                return;
            }
            if (normalizedKey.contains("source")
                    || normalizedKey.contains("origem")
                    || normalizedKey.contains("tribunal")
                    || normalizedKey.contains("authority")
                    || normalizedKey.contains("provider")
                    || normalizedKey.contains("url")
                    || normalizedKey.contains("link")
                    || normalizedKey.contains("workspace")
                    || normalizedKey.contains("gabinete")
                    || normalizedKey.contains("secretaria")
                    || normalizedKey.contains("institu")
                    || normalizedKey.contains("office")
                    || normalizedKey.contains("evidence")
                    || normalizedKey.contains("document")) {
                collectValue(value, out);
            }
        });
        return out;
    }

    private void collectValue(Object value, Set<String> out) {
        if (value == null) {
            return;
        }
        if (value instanceof String text) {
            String normalized = text.trim();
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
            return;
        }
        if (value instanceof Map<?, ?> nested) {
            nested.values().forEach(item -> collectValue(item, out));
            return;
        }
        if (value instanceof List<?> list) {
            list.forEach(item -> collectValue(item, out));
            return;
        }
        out.add(String.valueOf(value));
    }

    private void classifyCandidate(String candidate,
                                   Set<String> officialEvidenceIds,
                                   Set<String> institutionalControlledEvidenceIds,
                                   Set<String> derivedEvidenceIds,
                                   Set<String> untrustedEvidenceIds) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }
        if (officialEvidenceIds.contains(candidate) || untrustedEvidenceIds.contains(candidate)) {
            return;
        }
        if (isOfficial(candidate)) {
            officialEvidenceIds.add(candidate);
            return;
        }
        if (isInstitutionalControlled(candidate)) {
            institutionalControlledEvidenceIds.add(candidate);
            return;
        }
        if (isDerived(candidate)) {
            derivedEvidenceIds.add(candidate);
            return;
        }
        derivedEvidenceIds.add(candidate);
    }

    private boolean isOfficial(String candidate) {
        String normalized = candidate.trim().toUpperCase(Locale.ROOT);
        if (OFFICIAL_CODES.contains(normalized) || normalized.startsWith("PJB/") || normalized.startsWith("CNJ/")) {
            return true;
        }
        if (looksLikeUrl(candidate)) {
            String host = resolveHost(candidate);
            return host != null && (host.endsWith(".jus.br") || host.endsWith(".gov.br") || host.endsWith(".leg.br"));
        }
        return normalized.endsWith(".JUS.BR") || normalized.endsWith(".GOV.BR") || normalized.endsWith(".LEG.BR");
    }

    private boolean isInstitutionalControlled(String candidate) {
        String normalized = normalize(candidate);
        return normalized != null && INSTITUTIONAL_MARKERS.stream().anyMatch(normalized::contains);
    }

    private boolean isDerived(String candidate) {
        String normalized = normalize(candidate);
        return normalized != null && DERIVED_MARKERS.stream().anyMatch(normalized::contains);
    }

    private boolean looksLikeUrl(String value) {
        String normalized = normalize(value);
        return normalized != null && (normalized.startsWith("http://") || normalized.startsWith("https://"));
    }

    private String resolveHost(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String resolveTier(Set<String> officialEvidenceIds,
                               Set<String> institutionalControlledEvidenceIds,
                               Set<String> derivedEvidenceIds,
                               Set<String> untrustedEvidenceIds) {
        if (!untrustedEvidenceIds.isEmpty()) {
            return "UNTRUSTED_DOCUMENT";
        }
        if (!derivedEvidenceIds.isEmpty()) {
            return "DERIVED_DOCUMENT";
        }
        if (!institutionalControlledEvidenceIds.isEmpty()) {
            return "INSTITUTIONAL_CONTROLLED_DOCUMENT";
        }
        if (!officialEvidenceIds.isEmpty()) {
            return "OFFICIAL_DOCUMENT";
        }
        return "NO_EVIDENCE";
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    public record EvidenceTrustDecision(
            String tier,
            List<String> officialEvidenceIds,
            List<String> institutionalControlledEvidenceIds,
            List<String> derivedEvidenceIds,
            List<String> untrustedEvidenceIds,
            List<String> reasons,
            Map<String, Object> diagnostics
    ) {
        public EvidenceTrustDecision {
            Objects.requireNonNull(tier, "tier");
            officialEvidenceIds = officialEvidenceIds == null ? List.of() : List.copyOf(officialEvidenceIds);
            institutionalControlledEvidenceIds = institutionalControlledEvidenceIds == null ? List.of() : List.copyOf(institutionalControlledEvidenceIds);
            derivedEvidenceIds = derivedEvidenceIds == null ? List.of() : List.copyOf(derivedEvidenceIds);
            untrustedEvidenceIds = untrustedEvidenceIds == null ? List.of() : List.copyOf(untrustedEvidenceIds);
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
            diagnostics = diagnostics == null ? Map.of() : ImmutableViewSupport.map(diagnostics);
        }
    }
}
