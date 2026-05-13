package com.tcc.pjb.backend.ai.juridica.conversation.security;

import com.tcc.pjb.backend.ai.juridica.conversation.ImmutableViewSupport;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
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
public class LegalAttachmentProvenanceClassifier {

    private static final List<String> OFFICIAL_MARKERS = List.of(
            "acordao",
            "decisao",
            "despacho",
            "sentenca",
            "certidao",
            "mandado",
            "oficio",
            "edital",
            "ata",
            "laudo",
            "inteiro_teor",
            "inteiro-teor",
            "assinad",
            "signed"
    );
    private static final List<String> INSTITUTIONAL_MARKERS = List.of(
            "minuta",
            "rascunho",
            "draft",
            "gabinete",
            "secretaria",
            "institucional",
            "workspace",
            "office",
            "procuradoria",
            "defensoria",
            "promotoria",
            "parecer"
    );
    private static final List<String> DERIVED_MARKERS = List.of(
            "ocr",
            "resumo",
            "sumario",
            "sintese",
            "transcricao",
            "transcription",
            "anotacao",
            "nota",
            "notes",
            "extra",
            "consolidado",
            "merged"
    );

    public AttachmentProvenanceDecision classify(LegalAiConversationRequest request,
                                                 LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                 LegalAiConversationTrustZoneSnapshot trustZone) {
        List<String> attachments = request == null || request.attachments() == null ? List.of() : request.attachments();
        Set<String> allowedAttachments = new LinkedHashSet<>(documentSecurity == null || documentSecurity.allowedAttachments() == null ? attachments : documentSecurity.allowedAttachments());
        Set<String> quarantinedAttachments = new LinkedHashSet<>(documentSecurity == null || documentSecurity.quarantinedAttachments() == null ? List.of() : documentSecurity.quarantinedAttachments());
        LinkedHashSet<String> officialEvidenceIds = new LinkedHashSet<>();
        LinkedHashSet<String> institutionalControlledEvidenceIds = new LinkedHashSet<>();
        LinkedHashSet<String> derivedEvidenceIds = new LinkedHashSet<>();
        LinkedHashSet<String> untrustedEvidenceIds = new LinkedHashSet<>(quarantinedAttachments);
        allowedAttachments.forEach(attachment -> classifyAttachment(attachment, officialEvidenceIds, institutionalControlledEvidenceIds, derivedEvidenceIds));
        String tier = resolveTier(officialEvidenceIds, institutionalControlledEvidenceIds, derivedEvidenceIds, untrustedEvidenceIds);
        List<String> reasons = new ArrayList<>();
        if (!officialEvidenceIds.isEmpty()) {
            reasons.add("A malha detectou anexo com assinatura, ato ou forma oficial apta a ancorar promoção soberana.");
        }
        if (!institutionalControlledEvidenceIds.isEmpty()) {
            reasons.add("A malha detectou anexo institucional controlado e impôs fronteira soberana antes de minuta automática.");
        }
        if (!derivedEvidenceIds.isEmpty()) {
            reasons.add("A malha detectou anexo derivado e restringiu promoção automática para grounding, RAG e suggestion flow.");
        }
        if (!untrustedEvidenceIds.isEmpty()) {
            reasons.add("A malha detectou anexo não confiável ou em quarentena e travou promoção documental sensível.");
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("trustZone", trustZone == null ? null : trustZone.trustZone());
        diagnostics.put("trustZoneStatus", trustZone == null ? null : trustZone.status());
        diagnostics.put("attachmentEvidenceTier", tier);
        diagnostics.put("officialAttachmentCount", officialEvidenceIds.size());
        diagnostics.put("institutionalAttachmentCount", institutionalControlledEvidenceIds.size());
        diagnostics.put("derivedAttachmentCount", derivedEvidenceIds.size());
        diagnostics.put("untrustedAttachmentCount", untrustedEvidenceIds.size());
        return new AttachmentProvenanceDecision(
                tier,
                List.copyOf(officialEvidenceIds),
                List.copyOf(institutionalControlledEvidenceIds),
                List.copyOf(derivedEvidenceIds),
                List.copyOf(untrustedEvidenceIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private void classifyAttachment(String attachment,
                                    Set<String> officialEvidenceIds,
                                    Set<String> institutionalControlledEvidenceIds,
                                    Set<String> derivedEvidenceIds) {
        if (attachment == null || attachment.isBlank()) {
            return;
        }
        String normalized = attachment.trim().toLowerCase(Locale.ROOT);
        if (OFFICIAL_MARKERS.stream().anyMatch(normalized::contains)) {
            officialEvidenceIds.add(attachment);
            return;
        }
        if (INSTITUTIONAL_MARKERS.stream().anyMatch(normalized::contains)) {
            institutionalControlledEvidenceIds.add(attachment);
            return;
        }
        if (DERIVED_MARKERS.stream().anyMatch(normalized::contains)) {
            derivedEvidenceIds.add(attachment);
            return;
        }
        derivedEvidenceIds.add(attachment);
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

    public record AttachmentProvenanceDecision(
            String tier,
            List<String> officialEvidenceIds,
            List<String> institutionalControlledEvidenceIds,
            List<String> derivedEvidenceIds,
            List<String> untrustedEvidenceIds,
            List<String> reasons,
            Map<String, Object> diagnostics
    ) {
        public AttachmentProvenanceDecision {
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
