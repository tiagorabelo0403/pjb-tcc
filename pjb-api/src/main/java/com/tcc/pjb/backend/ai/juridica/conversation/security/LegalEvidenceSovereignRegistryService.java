package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalAttachmentProvenanceClassifier.AttachmentProvenanceDecision;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidencePromotionPolicy.EvidencePromotionDecision;
import com.tcc.pjb.backend.ai.juridica.conversation.security.LegalEvidenceTrustClassifier.EvidenceTrustDecision;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

@Service
public class LegalEvidenceSovereignRegistryService {

    public EvidenceRegistryDecision materialize(LegalAiConversationRequest request,
                                                LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                LegalAiConversationTrustZoneSnapshot trustZone,
                                                EvidenceTrustDecision sourceDecision,
                                                AttachmentProvenanceDecision attachmentDecision,
                                                EvidencePromotionDecision promotionDecision) {
        LinkedHashMap<String, LegalAiConversationEvidenceDescriptor> descriptors = new LinkedHashMap<>();
        materializeSources(descriptors, request, documentSecurity, trustZone, sourceDecision, promotionDecision);
        materializeAttachments(descriptors, request, documentSecurity, trustZone, attachmentDecision, promotionDecision);
        List<LegalAiConversationEvidenceDescriptor> orderedDescriptors = List.copyOf(descriptors.values());
        List<String> promotedRagEvidenceIds = collectPromotedIds(orderedDescriptors, LegalAiConversationEvidenceDescriptor::promotedForRag);
        List<String> promotedGroundingEvidenceIds = collectPromotedIds(orderedDescriptors, LegalAiConversationEvidenceDescriptor::promotedForGrounding);
        List<String> promotedDraftEvidenceIds = collectPromotedIds(orderedDescriptors, LegalAiConversationEvidenceDescriptor::promotedForDraft);
        List<String> promotedSuggestionEvidenceIds = collectPromotedIds(orderedDescriptors, LegalAiConversationEvidenceDescriptor::promotedForSuggestion);
        List<String> promotedCapabilityRecoveryEvidenceIds = collectPromotedIds(orderedDescriptors, LegalAiConversationEvidenceDescriptor::promotedForCapabilityRecovery);
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("descriptorCount", orderedDescriptors.size());
        diagnostics.put("promotedRagEvidenceIds", promotedRagEvidenceIds);
        diagnostics.put("promotedGroundingEvidenceIds", promotedGroundingEvidenceIds);
        diagnostics.put("promotedDraftEvidenceIds", promotedDraftEvidenceIds);
        diagnostics.put("promotedSuggestionEvidenceIds", promotedSuggestionEvidenceIds);
        diagnostics.put("promotedCapabilityRecoveryEvidenceIds", promotedCapabilityRecoveryEvidenceIds);
        diagnostics.put("trustZone", trustZone == null ? null : trustZone.trustZone());
        diagnostics.put("trustZoneStatus", trustZone == null ? null : trustZone.status());
        return new EvidenceRegistryDecision(
                orderedDescriptors,
                promotedRagEvidenceIds,
                promotedGroundingEvidenceIds,
                promotedDraftEvidenceIds,
                promotedSuggestionEvidenceIds,
                promotedCapabilityRecoveryEvidenceIds,
                ImmutableViewSupport.map(sanitizeNullValues(diagnostics))
        );
    }

    private void materializeSources(Map<String, LegalAiConversationEvidenceDescriptor> target,
                                    LegalAiConversationRequest request,
                                    LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                    LegalAiConversationTrustZoneSnapshot trustZone,
                                    EvidenceTrustDecision sourceDecision,
                                    EvidencePromotionDecision promotionDecision) {
        if (sourceDecision == null) {
            return;
        }
        sourceDecision.officialEvidenceIds().forEach(id -> target.put(id, descriptor(request, documentSecurity, trustZone, promotionDecision, id, "SOURCE", "OFFICIAL_DOCUMENT")));
        sourceDecision.institutionalControlledEvidenceIds().forEach(id -> target.put(id, descriptor(request, documentSecurity, trustZone, promotionDecision, id, "SOURCE", "INSTITUTIONAL_CONTROLLED_DOCUMENT")));
        sourceDecision.derivedEvidenceIds().forEach(id -> target.put(id, descriptor(request, documentSecurity, trustZone, promotionDecision, id, "SOURCE", "DERIVED_DOCUMENT")));
        sourceDecision.untrustedEvidenceIds().forEach(id -> target.put(id, descriptor(request, documentSecurity, trustZone, promotionDecision, id, "SOURCE", "UNTRUSTED_DOCUMENT")));
    }

    private void materializeAttachments(Map<String, LegalAiConversationEvidenceDescriptor> target,
                                        LegalAiConversationRequest request,
                                        LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                        LegalAiConversationTrustZoneSnapshot trustZone,
                                        AttachmentProvenanceDecision attachmentDecision,
                                        EvidencePromotionDecision promotionDecision) {
        if (attachmentDecision == null) {
            return;
        }
        attachmentDecision.officialEvidenceIds().forEach(id -> target.putIfAbsent(id, descriptor(request, documentSecurity, trustZone, promotionDecision, id, "ATTACHMENT", "OFFICIAL_DOCUMENT")));
        attachmentDecision.institutionalControlledEvidenceIds().forEach(id -> target.putIfAbsent(id, descriptor(request, documentSecurity, trustZone, promotionDecision, id, "ATTACHMENT", "INSTITUTIONAL_CONTROLLED_DOCUMENT")));
        attachmentDecision.derivedEvidenceIds().forEach(id -> target.putIfAbsent(id, descriptor(request, documentSecurity, trustZone, promotionDecision, id, "ATTACHMENT", "DERIVED_DOCUMENT")));
        attachmentDecision.untrustedEvidenceIds().forEach(id -> target.putIfAbsent(id, descriptor(request, documentSecurity, trustZone, promotionDecision, id, "ATTACHMENT", "UNTRUSTED_DOCUMENT")));
    }

    private LegalAiConversationEvidenceDescriptor descriptor(LegalAiConversationRequest request,
                                                             LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                             LegalAiConversationTrustZoneSnapshot trustZone,
                                                             EvidencePromotionDecision promotionDecision,
                                                             String evidenceId,
                                                             String evidenceKind,
                                                             String evidenceTier) {
        boolean quarantined = isQuarantined(evidenceId, documentSecurity);
        List<String> quarantineReasons = quarantineReasons(evidenceId, documentSecurity, quarantined);
        List<String> downgradeReasons = downgradeReasons(evidenceTier, trustZone, promotionDecision, quarantined);
        return new LegalAiConversationEvidenceDescriptor(
                evidenceId,
                evidenceKind,
                evidenceTier,
                resolveOriginType(evidenceId, evidenceKind),
                resolveIssuer(request, evidenceId, evidenceKind),
                resolveSourceReference(evidenceId),
                integrityHash(evidenceKind, evidenceId, evidenceTier),
                resolveSignatureStatus(evidenceId, evidenceKind, evidenceTier),
                resolveExtractionMode(evidenceId, evidenceTier),
                quarantined,
                quarantineReasons,
                downgradeReasons,
                promotedForRag(evidenceId, evidenceTier, promotionDecision),
                promotedForGrounding(evidenceId, evidenceTier, promotionDecision),
                promotedForDraft(evidenceId, evidenceTier, promotionDecision),
                promotedForSuggestion(evidenceId, evidenceTier, promotionDecision),
                promotedForCapabilityRecovery(evidenceId, evidenceTier, promotionDecision)
        );
    }

    private boolean promotedForRag(String evidenceId, String evidenceTier, EvidencePromotionDecision promotionDecision) {
        return isPromoted("PROMOTED", promotionDecision == null ? null : promotionDecision.ragPromotionStatus(), evidenceTier, evidenceId);
    }

    private boolean promotedForGrounding(String evidenceId, String evidenceTier, EvidencePromotionDecision promotionDecision) {
        return isPromoted("PROMOTED", promotionDecision == null ? null : promotionDecision.groundingPromotionStatus(), evidenceTier, evidenceId);
    }

    private boolean promotedForDraft(String evidenceId, String evidenceTier, EvidencePromotionDecision promotionDecision) {
        return isPromoted("PROMOTED", promotionDecision == null ? null : promotionDecision.draftPromotionStatus(), evidenceTier, evidenceId);
    }

    private boolean promotedForSuggestion(String evidenceId, String evidenceTier, EvidencePromotionDecision promotionDecision) {
        return isPromoted("PROMOTED", promotionDecision == null ? null : promotionDecision.suggestionPromotionStatus(), evidenceTier, evidenceId);
    }

    private boolean promotedForCapabilityRecovery(String evidenceId, String evidenceTier, EvidencePromotionDecision promotionDecision) {
        return isPromoted("PROMOTED", promotionDecision == null ? null : promotionDecision.capabilityRecoveryPromotionStatus(), evidenceTier, evidenceId);
    }

    private boolean isPromoted(String expectedStatus, String actualStatus, String evidenceTier, String evidenceId) {
        if (!Objects.equals(expectedStatus, actualStatus)) {
            return false;
        }
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(evidenceTier) || "DERIVED_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            return false;
        }
        return evidenceId != null && !evidenceId.isBlank();
    }

    private List<String> collectPromotedIds(List<LegalAiConversationEvidenceDescriptor> descriptors,
                                            Predicate<LegalAiConversationEvidenceDescriptor> predicate) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        descriptors.stream().filter(predicate).map(LegalAiConversationEvidenceDescriptor::evidenceId).filter(Objects::nonNull).forEach(out::add);
        return List.copyOf(out);
    }

    private boolean isQuarantined(String evidenceId, LegalAiConversationDocumentSecuritySnapshot documentSecurity) {
        if (evidenceId == null || evidenceId.isBlank() || documentSecurity == null) {
            return false;
        }
        return listContains(documentSecurity.quarantinedAttachments(), evidenceId)
                || listContains(documentSecurity.blockedSources(), evidenceId);
    }

    private boolean listContains(List<String> values, String candidate) {
        if (values == null || candidate == null) {
            return false;
        }
        return values.stream().anyMatch(candidate::equals);
    }

    private List<String> quarantineReasons(String evidenceId,
                                           LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                           boolean quarantined) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if (!quarantined) {
            return List.of();
        }
        if (documentSecurity != null && listContains(documentSecurity.quarantinedAttachments(), evidenceId)) {
            reasons.add("ATTACHMENT_QUARANTINED");
        }
        if (documentSecurity != null && listContains(documentSecurity.blockedSources(), evidenceId)) {
            reasons.add("SOURCE_BLOCKED");
        }
        if (reasons.isEmpty()) {
            reasons.add("SOVEREIGN_QUARANTINE");
        }
        return List.copyOf(reasons);
    }

    private List<String> downgradeReasons(String evidenceTier,
                                          LegalAiConversationTrustZoneSnapshot trustZone,
                                          EvidencePromotionDecision promotionDecision,
                                          boolean quarantined) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if ("DERIVED_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            reasons.add("DERIVED_CHAIN_REQUIRES_CONFIRMATION");
        }
        if ("INSTITUTIONAL_CONTROLLED_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            reasons.add("INSTITUTIONAL_CHAIN_REQUIRES_GOVERNANCE");
        }
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            reasons.add("UNTRUSTED_CHAIN_BLOCKED");
        }
        if (trustZone != null && trustZone.sovereignBoundaryRequired()) {
            reasons.add("SOVEREIGN_BOUNDARY_REQUIRED");
        }
        if (promotionDecision != null && "LOCKED".equalsIgnoreCase(promotionDecision.status())) {
            reasons.add("PROMOTION_LOCKED");
        }
        if (quarantined) {
            reasons.add("QUARANTINE_ACTIVE");
        }
        return List.copyOf(reasons);
    }

    private String resolveOriginType(String evidenceId, String evidenceKind) {
        if ("ATTACHMENT".equalsIgnoreCase(evidenceKind)) {
            return "ATTACHMENT_UPLOAD";
        }
        if (looksLikeUrl(evidenceId)) {
            return "REMOTE_SOURCE";
        }
        if (looksLikeOfficialCode(evidenceId)) {
            return "SYSTEM_SOURCE";
        }
        return "CONTEXT_SOURCE";
    }

    private String resolveIssuer(LegalAiConversationRequest request, String evidenceId, String evidenceKind) {
        if ("ATTACHMENT".equalsIgnoreCase(evidenceKind)) {
            String issuer = firstContextValue(request, "documentAuthority", "issuer", "authority", "sourceSystem");
            return issuer == null ? "ATTACHMENT_CHAIN" : issuer;
        }
        if (looksLikeUrl(evidenceId)) {
            return resolveHost(evidenceId);
        }
        String issuer = firstContextValue(request, "documentAuthority", "issuer", "authority", "sourceSystem", "tribunal", "provider");
        if (issuer != null) {
            return issuer;
        }
        return evidenceId;
    }

    private String resolveSourceReference(String evidenceId) {
        if (evidenceId == null || evidenceId.isBlank()) {
            return null;
        }
        return evidenceId;
    }

    private String resolveSignatureStatus(String evidenceId, String evidenceKind, String evidenceTier) {
        String normalized = normalize(evidenceId);
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            return "UNVERIFIED";
        }
        if ("ATTACHMENT".equalsIgnoreCase(evidenceKind)
                && normalized != null
                && (normalized.contains("assinad") || normalized.contains("signed") || normalized.contains("pades") || normalized.contains("pkcs7"))) {
            return "SIGNED";
        }
        if ("OFFICIAL_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            return "SYSTEM_VERIFIED";
        }
        if ("INSTITUTIONAL_CONTROLLED_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            return "CONTROLLED_PENDING_CONFIRMATION";
        }
        return "UNKNOWN";
    }

    private String resolveExtractionMode(String evidenceId, String evidenceTier) {
        String normalized = normalize(evidenceId);
        if ("UNTRUSTED_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            return "QUARANTINED_OR_EXTERNAL";
        }
        if ("DERIVED_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            if (normalized != null && (normalized.contains("ocr") || normalized.contains("transcri") || normalized.contains("extra"))) {
                return "OCR_DERIVED";
            }
            return "DERIVED";
        }
        if ("INSTITUTIONAL_CONTROLLED_DOCUMENT".equalsIgnoreCase(evidenceTier)) {
            return "INSTITUTIONAL_CONTROLLED";
        }
        return "DIRECT_OFFICIAL";
    }

    private String firstContextValue(LegalAiConversationRequest request, String... keys) {
        if (request == null || request.context() == null || request.context().isEmpty()) {
            return null;
        }
        for (String key : keys) {
            Object value = request.context().get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return null;
    }

    private String integrityHash(String evidenceKind, String evidenceId, String evidenceTier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((String.valueOf(evidenceKind) + "|" + String.valueOf(evidenceId) + "|" + String.valueOf(evidenceTier)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível para materializar evidência soberana", ex);
        }
    }

    private boolean looksLikeUrl(String value) {
        String normalized = normalize(value);
        return normalized != null && (normalized.startsWith("http://") || normalized.startsWith("https://"));
    }

    private boolean looksLikeOfficialCode(String value) {
        String normalized = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        return normalized != null && !normalized.isBlank() && normalized.equals(normalized.replaceAll("[^A-Z0-9_.\\-\\/]", ""));
    }

    private String resolveHost(String value) {
        try {
            return URI.create(value).getHost();
        } catch (Exception ex) {
            return value;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private Map<String, Object> sanitizeNullValues(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(key, value);
            }
        });
        return out;
    }

    public record EvidenceRegistryDecision(
            List<LegalAiConversationEvidenceDescriptor> descriptors,
            List<String> promotedRagEvidenceIds,
            List<String> promotedGroundingEvidenceIds,
            List<String> promotedDraftEvidenceIds,
            List<String> promotedSuggestionEvidenceIds,
            List<String> promotedCapabilityRecoveryEvidenceIds,
            Map<String, Object> diagnostics
    ) {
        public EvidenceRegistryDecision {
            descriptors = descriptors == null ? List.of() : List.copyOf(descriptors);
            promotedRagEvidenceIds = promotedRagEvidenceIds == null ? List.of() : List.copyOf(promotedRagEvidenceIds);
            promotedGroundingEvidenceIds = promotedGroundingEvidenceIds == null ? List.of() : List.copyOf(promotedGroundingEvidenceIds);
            promotedDraftEvidenceIds = promotedDraftEvidenceIds == null ? List.of() : List.copyOf(promotedDraftEvidenceIds);
            promotedSuggestionEvidenceIds = promotedSuggestionEvidenceIds == null ? List.of() : List.copyOf(promotedSuggestionEvidenceIds);
            promotedCapabilityRecoveryEvidenceIds = promotedCapabilityRecoveryEvidenceIds == null ? List.of() : List.copyOf(promotedCapabilityRecoveryEvidenceIds);
            diagnostics = diagnostics == null ? Map.of() : ImmutableViewSupport.map(diagnostics);
        }
    }
}
