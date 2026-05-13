package com.tcc.pjb.backend.service.upload;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;

@Service
public class UploadTokenService {

    private final ObjectStorageProperties props;

    public UploadTokenService(ObjectStorageProperties props) {
        this.props = props;
    }

    public String issue(UUID batchId, UUID itemId, String storageKey, Instant expiresAt) {
        Objects.requireNonNull(batchId, "batchId");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(storageKey, "storageKey");
        Objects.requireNonNull(expiresAt, "expiresAt");

        String payload = batchId + "|" + itemId + "|" + storageKey + "|" + expiresAt.getEpochSecond();
        String sig = sign(payload);
        String token = payload + "|" + sig;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public VerifiedToken verify(String tokenB64) {
        Objects.requireNonNull(tokenB64, "token");
        byte[] raw;
        try {
            raw = Base64.getUrlDecoder().decode(tokenB64);
        } catch (IllegalArgumentException e) {
            throw new SecurityException("token inválido");
        }

        String token = new String(raw, StandardCharsets.UTF_8);
        String[] parts = token.split("\\|", -1);
        if (parts.length != 5) {
            throw new SecurityException("token inválido");
        }

        UUID batchId = parseUuid(parts[0]);
        UUID itemId = parseUuid(parts[1]);
        String key = parts[2];
        long exp = parseLong(parts[3]);
        String sig = parts[4];

        String payload = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3];
        String expected = sign(payload);

        if (!constantTimeEquals(sig, expected)) {
            throw new SecurityException("assinatura inválida");
        }

        Instant expiresAt = Instant.ofEpochSecond(exp);
        if (Instant.now().isAfter(expiresAt)) {
            throw new SecurityException("token expirado");
        }

        return new VerifiedToken(batchId, itemId, key, expiresAt);
    }

    private String sign(String payload) {
        try {
            byte[] keyBytes = props.getUpload().getTokenSecret().getBytes(StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("falha ao assinar token", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            throw new SecurityException("uuid inválido");
        }
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            throw new SecurityException("exp inválida");
        }
    }

    public record VerifiedToken(UUID batchId, UUID itemId, String storageKey, Instant expiresAt) {
    }
}
