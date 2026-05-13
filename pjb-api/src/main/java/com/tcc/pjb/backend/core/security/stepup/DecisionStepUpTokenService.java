package com.tcc.pjb.backend.core.security.stepup;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DecisionStepUpTokenService {

    public static final String PREFIX = "PJB-DECISION-STEPUPv1";

    private final ObjectMapper objectMapper;
    private final KeyMaterialService keyMaterialService;

    public DecisionStepUpTokenService(ObjectMapper objectMapper, KeyMaterialService keyMaterialService) {
        this.objectMapper = objectMapper;
        this.keyMaterialService = keyMaterialService;
    }

    public String sign(DecisionStepUpTokenPayload payload) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            String p64 = base64Url(json);
            String signingInput = PREFIX + "." + p64;
            byte[] sig = hmac(signingInput.getBytes(StandardCharsets.UTF_8), keyMaterialService.getFaceReauthSigningKey());
            return signingInput + "." + base64Url(sig);
        } catch (Exception e) {
            log.error("CRITICAL: falha ao assinar token decisório", e);
            throw new IllegalStateException("Falha crítica ao assinar token decisório", e);
        }
    }

    public DecisionStepUpTokenPayload verifyAndDecode(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token decisório ausente");
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("formato inválido");
        }
        if (!PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("prefixo inválido");
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] sigProvided = base64UrlDecode(parts[2]);
        byte[] sigExpected = hmac(signingInput.getBytes(StandardCharsets.UTF_8), keyMaterialService.getFaceReauthSigningKey());
        if (!MessageDigest.isEqual(sigProvided, sigExpected)) {
            throw new IllegalArgumentException("assinatura inválida");
        }
        try {
            DecisionStepUpTokenPayload payload = objectMapper.readValue(base64UrlDecode(parts[1]), DecisionStepUpTokenPayload.class);
            if (payload.userId() == null || payload.actType() == null || payload.actType().isBlank() || payload.focusSessionId() == null) {
                throw new IllegalArgumentException("payload inválido");
            }
            return payload;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("payload inválido", e);
        }
    }

    private static byte[] hmac(byte[] message, SecretKey key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getEncoded(), "HmacSHA256"));
            return mac.doFinal(message);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(String b64) {
        return Base64.getUrlDecoder().decode(b64);
    }
}
