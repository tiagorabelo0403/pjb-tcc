package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class JuridicaHallucinationGuardService {

    private static final Pattern ARTICLE_PATTERN = Pattern.compile("\\bart\\.?\\s*\\d+[A-Za-z0-9º°.-]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRECEDENT_PATTERN = Pattern.compile("\\b(REsp|AREsp|RE|ARE|HC|RHC|RMS|Tema|S[uú]mula)\\b", Pattern.CASE_INSENSITIVE);

    private final JuridicaLegalAiSpineService juridicaLegalAiSpineService;

    public JuridicaHallucinationGuardService(JuridicaLegalAiSpineService juridicaLegalAiSpineService) {
        this.juridicaLegalAiSpineService = Objects.requireNonNull(juridicaLegalAiSpineService, "juridicaLegalAiSpineService");
    }

    public LegalHallucinationGuardResponse evaluate(LegalHallucinationGuardRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (request != null) {
            payload.put("ramo", request.ramo());
            payload.put("rito", request.rito());
            payload.put("classe", request.classe());
            if (request.filtros() != null) {
                payload.putAll(request.filtros());
            }
            payload.put("groundedCitationsCount", request.groundedCitations() == null ? 0 : request.groundedCitations().size());
        }

        var spine = juridicaLegalAiSpineService.resolveForSkill(
                JuridicaSpineLabels.CAPABILITY_HALLUCINATION_GUARD,
                ApiVersion.V3,
                payload
        );

        String text = request == null || request.texto() == null ? "" : request.texto().trim();
        List<String> grounded = request == null || request.groundedCitations() == null
                ? List.of()
                : request.groundedCitations().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        List<String> suspiciousSignals = new ArrayList<>();
        List<String> blockedReasons = new ArrayList<>();

        boolean containsArticleReference = containsPattern(ARTICLE_PATTERN, text);
        boolean containsPrecedentReference = containsPattern(PRECEDENT_PATTERN, text);
        boolean hasGroundedCitations = !grounded.isEmpty();

        if (text.isBlank()) {
            blockedReasons.add("Texto ausente para validacao anti-alucinacao.");
        }
        if (containsArticleReference && !hasGroundedCitations && spine.hallucinationGuard().articleReferenceVerificationRequired()) {
            blockedReasons.add("Referencia de artigo sem base confirmada no conjunto grounded.");
        }
        if (containsPrecedentReference && !hasGroundedCitations && spine.hallucinationGuard().precedentVerificationRequired()) {
            blockedReasons.add("Referencia jurisprudencial sem base confirmada no conjunto grounded.");
        }
        if (containsSuspiciousPhrase(text, spine.hallucinationGuard().suspiciousPatterns())) {
            suspiciousSignals.add("Linguagem de autoridade generica sem fonte verificavel.");
        }
        if (!text.isBlank() && !containsArticleReference && !containsPrecedentReference && grounded.isEmpty()) {
            suspiciousSignals.add("Texto juridico sem lastro normativo ou jurisprudencial verificavel.");
        }
        if (text.contains(JuridicaSpineLabels.UNRESOLVED_CITATION_PLACEHOLDER)) {
            suspiciousSignals.add("Resposta marcou citacao nao confirmada; exigir grounding antes de uso operacional.");
        }

        String status = blockedReasons.isEmpty()
                ? suspiciousSignals.isEmpty() ? "ALIGNED" : "REVIEW_REQUIRED"
                : "BLOCKED";

        LinkedHashMap<String, Object> trace = new LinkedHashMap<>();
        trace.put("lane", spine.trace().lane());
        trace.put("auditFields", spine.trace().requiredAuditFields());
        trace.put("citationEmissionMode", spine.hallucinationGuard().citationEmissionMode());
        trace.put("groundedCitationCount", grounded.size());
        trace.put("containsArticleReference", containsArticleReference);
        trace.put("containsPrecedentReference", containsPrecedentReference);
        trace.put("blockedByUngroundedNormativeClaims", !blockedReasons.isEmpty());

        return new LegalHallucinationGuardResponse(
                spine.profileCode(),
                spine.version(),
                spine.capability(),
                status,
                spine.hallucinationGuard().articleReferenceVerificationRequired(),
                spine.hallucinationGuard().precedentVerificationRequired(),
                spine.hallucinationGuard().freeFormCitationBlocked(),
                spine.hallucinationGuard().citationEmissionMode(),
                spine.hallucinationGuard().unresolvedCitationPlaceholder(),
                null,
                null,
                null,
                null,
                List.copyOf(suspiciousSignals),
                List.copyOf(blockedReasons),
                Map.copyOf(trace)
        );
    }

    private boolean containsPattern(Pattern pattern, String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find();
    }

    private boolean containsSuspiciousPhrase(String text, List<String> suspiciousPatterns) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return suspiciousPatterns != null && suspiciousPatterns.stream()
                .filter(Objects::nonNull)
                .map(pattern -> pattern.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }
}
