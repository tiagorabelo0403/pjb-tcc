package com.tcc.pjb.backend.core.frontend.publicaccess;

import java.util.ArrayList;
import java.util.List;

public final class PjbPublicDocumentTrustCenter {

    public PjbDocumentTrustStatus evaluate(PjbPublicDocumentTrustEvidence evidence) {
        if (evidence == null) {
            return PjbDocumentTrustStatus.SIGNATURE_REVIEW_REQUIRED;
        }
        if (evidence.revoked()) {
            return PjbDocumentTrustStatus.REVOKED;
        }
        if (!evidence.expectedSha256().isBlank() && !evidence.expectedSha256().equalsIgnoreCase(evidence.actualSha256())) {
            return PjbDocumentTrustStatus.HASH_MISMATCH;
        }
        if (!evidence.signatureValid()) {
            return PjbDocumentTrustStatus.SIGNATURE_REVIEW_REQUIRED;
        }
        if (!evidence.timestampPresent()) {
            return PjbDocumentTrustStatus.TIMESTAMP_REVIEW_REQUIRED;
        }
        if (!evidence.publicVersionAvailable()) {
            return PjbDocumentTrustStatus.PUBLIC_REDACTION_REQUIRED;
        }
        return PjbDocumentTrustStatus.TRUSTED;
    }

    public List<String> publicExplanation(PjbPublicDocumentTrustEvidence evidence) {
        PjbDocumentTrustStatus status = evaluate(evidence);
        List<String> messages = new ArrayList<>();
        messages.add(switch (status) {
            case TRUSTED -> "documento íntegro, assinado e verificável para consulta pública";
            case PUBLIC_REDACTION_REQUIRED -> "documento íntegro, mas ainda exige versão pública com proteção de sigilo";
            case SIGNATURE_REVIEW_REQUIRED -> "assinatura digital ausente ou pendente de revisão";
            case TIMESTAMP_REVIEW_REQUIRED -> "carimbo temporal ausente ou pendente de revisão";
            case HASH_MISMATCH -> "hash informado não corresponde ao documento verificado";
            case REVOKED -> "documento revogado ou substituído por versão posterior";
        });
        if (evidence != null) {
            messages.addAll(evidence.publicMessages());
        }
        return List.copyOf(messages);
    }
}
