package com.tcc.pjb.backend.service.ministro.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;

@Service
public class PlenarioVoteCryptographyService {

    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper;

    public PlenarioVoteCryptographyService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public SealedVote sealVote(String sessaoCodigo,
                               Long ministroId,
                               String votoOpcao,
                               String fundamentacaoResumo,
                               String ressalva) {
        String nonce = randomToken(16);
        String keySeed = sessionKeySeed(sessaoCodigo);
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessao", sessaoCodigo);
        payload.put("ministroId", ministroId);
        payload.put("votoOpcao", votoOpcao);
        payload.put("fundamentacaoResumo", fundamentacaoResumo);
        payload.put("ressalva", ressalva);
        payload.put("nonce", nonce);
        payload.put("iat", Instant.now().getEpochSecond());

        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            byte[] cipher = encrypt(payload, keySeed, iv, sessaoCodigo);
            LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("iv", URL_ENCODER.encodeToString(iv));
            envelope.put("cipher", URL_ENCODER.encodeToString(cipher));
            envelope.put("alg", "AES/GCM/NoPadding");
            String envelopeBase64 = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(envelope));
            String commitmentHash = Hashes.sha256Hex(sessaoCodigo + "|" + ministroId + "|" + votoOpcao + "|" + nonce);
            String integrityProof = Hashes.sha256Hex(commitmentHash + "|" + envelopeBase64 + "|" + keySeed);
            String receiptHash = Hashes.sha256Hex(integrityProof + "|" + sessaoCodigo);
            return new SealedVote(envelopeBase64, commitmentHash, receiptHash, integrityProof);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar voto do plenário.", e);
        }
    }

    public RevealedVote revealVote(String sessaoCodigo, String envelopeBase64) {
        try {
            byte[] envelopeBytes = URL_DECODER.decode(envelopeBase64);
            Map<String, Object> envelope = objectMapper.readValue(envelopeBytes, MAP_TYPE);
            byte[] iv = URL_DECODER.decode(String.valueOf(envelope.get("iv")));
            byte[] cipher = URL_DECODER.decode(String.valueOf(envelope.get("cipher")));
            byte[] plaintext = decrypt(cipher, sessionKeySeed(sessaoCodigo), iv, sessaoCodigo);
            Map<String, Object> payload = objectMapper.readValue(plaintext, MAP_TYPE);
            return new RevealedVote(
                    String.valueOf(payload.get("votoOpcao")),
                    String.valueOf(payload.getOrDefault("fundamentacaoResumo", "")),
                    String.valueOf(payload.getOrDefault("ressalva", "")),
                    String.valueOf(payload.getOrDefault("nonce", "")),
                    String.valueOf(payload.getOrDefault("iat", "0"))
            );
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao revelar voto do plenário.", e);
        }
    }

    private byte[] encrypt(Map<String, Object> payload, String keySeed, byte[] iv, String aad) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key(keySeed), new javax.crypto.spec.GCMParameterSpec(128, iv));
        cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        return cipher.doFinal(objectMapper.writeValueAsBytes(payload));
    }

    private byte[] decrypt(byte[] cipherBytes, String keySeed, byte[] iv, String aad) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key(keySeed), new javax.crypto.spec.GCMParameterSpec(128, iv));
        cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        return cipher.doFinal(cipherBytes);
    }

    private javax.crypto.SecretKey key(String seed) {
        byte[] digest = Hashes.sha256(seed.getBytes(StandardCharsets.UTF_8));
        return new javax.crypto.spec.SecretKeySpec(digest, 0, 16, "AES");
    }

    private String sessionKeySeed(String sessaoCodigo) {
        return "PJB-PLENARIO-2026|" + sessaoCodigo + "|SEAL";
    }

    private String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return URL_ENCODER.encodeToString(buffer);
    }

    public record SealedVote(
            String envelopeBase64,
            String commitmentHash,
            String receiptHash,
            String integrityProof
    ) {
    }

    public record RevealedVote(
            String votoOpcao,
            String fundamentacaoResumo,
            String ressalva,
            String nonce,
            String emittedAt
    ) {
    }
}
