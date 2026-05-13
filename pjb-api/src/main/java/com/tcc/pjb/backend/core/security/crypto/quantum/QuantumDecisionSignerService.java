package com.tcc.pjb.backend.core.security.crypto.quantum;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeSentencaDraft;

@Service
public class QuantumDecisionSignerService {

    private static final Logger log = LoggerFactory.getLogger(QuantumDecisionSignerService.class);

    private final PjbQuantumProperties props;

    public QuantumDecisionSignerService(PjbQuantumProperties props) {
        this.props = props;
    }

    public Optional<PqcEvidence> signAndAttachEvidenceOrThrowIfEnabled(LaianeSentencaDraft draft) {
        if (draft == null) return Optional.empty();
        if (!props.enabled()) return Optional.empty();

        
        PostQuantumSigner signer = new PostQuantumSigner(props.signatureAlgorithm());

        byte[] payload = canonicalPayload(draft);
        PqcEvidence evidence = signer.sign(payload);

        
        
        log.info("PQC_EVIDENCE_SENTENCA draftId={} uuid={} alg={} payloadSha256={} sigB64.len={} pubB64.len={}",
                draft.getId(),
                draft.getUuid(),
                evidence.algorithm(),
                sha256Hex(payload),
                evidence.signatureB64() != null ? evidence.signatureB64().length() : 0,
                evidence.publicKeyB64() != null ? evidence.publicKeyB64().length() : 0
        );

        
        boolean ok = signer.verify(payload, evidence);
        if (!ok) {
            throw new IllegalStateException("Falha de verificação interna da assinatura PQC (autocheck). Publicação negada.");
        }

        return Optional.of(evidence);
    }

    private static byte[] canonicalPayload(LaianeSentencaDraft d) {
        
        StringBuilder sb = new StringBuilder(4096);
        sb.append("PJB|SENTENCA|V1\n");
        sb.append("draftId=").append(d.getId()).append('\n');
        sb.append("uuid=").append(d.getUuid()).append('\n');
        if (d.getProcesso() != null) sb.append("processoId=").append(d.getProcesso().getId()).append('\n');
        if (d.getInputHash() != null) sb.append("inputHash=").append(d.getInputHash()).append('\n');
        sb.append("draftMarkdown=\n").append(nullToEmpty(d.getDraftMarkdown())).append("\n");
        sb.append("contextJson=\n").append(nullToEmpty(d.getContextJson())).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "sha256_error";
        }
    }
}
