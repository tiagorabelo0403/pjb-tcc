package com.tcc.pjb.backend.integration.n8n;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class N8nSignatureService {

    public String sign(String secret, String payload) {
        String effectiveSecret = normalizeSecret(secret);
        String effectivePayload = payload == null ? "" : payload;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(effectiveSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(effectivePayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao assinar payload n8n.", e);
        }
    }

    public boolean matches(String secret, String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        byte[] expected = sign(secret, payload).getBytes(StandardCharsets.UTF_8);
        byte[] provided = signature.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    private String normalizeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Segredo HMAC do n8n não configurado.");
        }
        return secret.trim();
    }
}
