package com.tcc.pjb.backend.core.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CryptoVaultService {

    private static final String LEGACY_PLAINTEXT_PREFIX = "PLAIN:";

    private final String masterKeyBase64;
    private final boolean allowPlaintextFallback;

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public CryptoVaultService(
            @Value("${pjb.security.master-key:}") String masterKeyBase64,
            @Value("${pjb.security.crypto.allow-plaintext-fallback:false}") boolean allowPlaintextFallback
    ) {
        this.masterKeyBase64 = masterKeyBase64;
        this.allowPlaintextFallback = allowPlaintextFallback;
    }

    public String blindarDado(String dadoSensivel) {
        if (dadoSensivel == null) return null;

        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            if (allowPlaintextFallback) {
                return LEGACY_PLAINTEXT_PREFIX + Base64.getEncoder().encodeToString(dadoSensivel.getBytes(StandardCharsets.UTF_8));
            }
            throw new IllegalStateException("pjb.security.master-key ausente: configure a chave mestra (Base64, >= 32 bytes)");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(dadoSensivel.getBytes(StandardCharsets.UTF_8));
            byte[] message = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, message, 0, iv.length);
            System.arraycopy(cipherText, 0, message, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(message);

        } catch (Exception e) {
            log.error("CRITICAL: falha ao criptografar dado sensível", e);
            throw new SecurityException("Erro interno de criptografia");
        }
    }

    /**
     * Índice cego: hash determinístico (HMAC-SHA256 com a mesma chave mestra) para permitir busca
     * exata por igualdade em coluna cujo valor de exibição é criptografado ({@link #blindarDado}, que
     * usa IV aleatório e por isso nunca é comparável em {@code WHERE}). Nunca usar para dados que
     * precisem de busca parcial (LIKE) — hash não suporta isso.
     */
    public String hmacHex(String valor) {
        if (valor == null) return null;

        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            if (allowPlaintextFallback) {
                return com.tcc.pjb.backend.core.util.Hashes.sha256Hex(valor);
            }
            throw new IllegalStateException("pjb.security.master-key ausente: configure a chave mestra (Base64, >= 32 bytes)");
        }

        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(getSecretKey().getEncoded(), "HmacSHA256"));
            byte[] digest = mac.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format(java.util.Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("CRITICAL: falha ao calcular índice cego", e);
            throw new SecurityException("Erro interno de índice cego");
        }
    }

    public String lerDadoBlindado(String dadoCifradoBase64) {
        if (dadoCifradoBase64 == null) return null;

        if (dadoCifradoBase64.startsWith(LEGACY_PLAINTEXT_PREFIX)) {
            String base64 = dadoCifradoBase64.substring(LEGACY_PLAINTEXT_PREFIX.length());
            try {
                return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return base64;
            }
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(dadoCifradoBase64);
        } catch (IllegalArgumentException e) {
            
            return dadoCifradoBase64;
        }

        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            if (allowPlaintextFallback) {
                
                return dadoCifradoBase64;
            }
            throw new IllegalStateException("pjb.security.master-key ausente: não é possível descriptografar dados sensíveis");
        }

        try {
            if (decoded.length < GCM_IV_LENGTH + 16) {
                throw new SecurityException("ciphertext inválido");
            }

            GCMParameterSpec params = new GCMParameterSpec(GCM_TAG_LENGTH, decoded, 0, GCM_IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), params);

            byte[] plainText = cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.length - GCM_IV_LENGTH);
            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("CRITICAL: falha ao descriptografar dado", e);
            throw new SecurityException("Erro interno de descriptografia");
        }
    }

    private static boolean isWeakMasterKey(byte[] decodedKey) {
        if (decodedKey == null || decodedKey.length == 0) {
            return true;
        }
        byte first = decodedKey[0];
        boolean repeated = true;
        for (byte value : decodedKey) {
            if (value != first) {
                repeated = false;
                break;
            }
        }
        return repeated;
    }

    private SecretKey getSecretKey() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(masterKeyBase64);
            if (decodedKey.length < 32) {
                throw new IllegalStateException("pjb.security.master-key precisa ter >= 32 bytes (Base64)");
            }
            if (isWeakMasterKey(decodedKey)) {
                throw new IllegalStateException("pjb.security.master-key insegura: configure uma chave aleatória forte e exclusiva");
            }
            return new SecretKeySpec(decodedKey, 0, 32, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Chave mestra inválida", e);
        }
    }
}
