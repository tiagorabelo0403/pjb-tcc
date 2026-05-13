package com.tcc.pjb.backend.core.security.edge;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;

@Service
public class EdgeAttestationService {

    private final ObjectMapper objectMapper;

    public EdgeAttestationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public NormalizedAttestation normalizeOrThrow(String rawJson) {
        return normalizeOrThrow(parseRaw(rawJson));
    }

    public NormalizedAttestation normalizeOrThrow(String rawJson, String expectedHashSha384, long expectedSizeBytes) {
        return normalizeOrThrow(parseRaw(rawJson), expectedHashSha384, expectedSizeBytes);
    }

    public NormalizedAttestation normalizeOrThrow(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return new NormalizedAttestation("{}", "{}", null, Instant.EPOCH, false, false);
        }

        String schema = s(raw.get("schema"));
        String hashAlg = s(raw.get("hashAlgorithm"));
        String hash = s(raw.get("hashSha384"));
        String size = s(raw.get("size"));
        String signedAt = s(raw.get("signedAt"));
        String signatureB64 = s(raw.get("signatureB64"));
        String certPem = s(raw.get("certificatePem"));

        Instant ts = parseInstant(signedAt);

        Map<String, Object> payload = new LinkedHashMap<>();
        if (!schema.isBlank()) payload.put("schema", schema);
        if (!hashAlg.isBlank()) payload.put("hashAlgorithm", hashAlg);
        if (!hash.isBlank()) payload.put("hashSha384", hash);
        if (!size.isBlank()) payload.put("size", size);
        payload.put("signedAt", ts.toString());

        String payloadJson = writeJson(payload);

        boolean hasSig = !signatureB64.isBlank() && !certPem.isBlank();
        boolean verified = false;
        String subject = null;

        if (hasSig) {
            X509Certificate cert = parseX509(certPem);
            subject = cert.getSubjectX500Principal() != null ? cert.getSubjectX500Principal().getName() : null;
            verifySignature(payloadJson, signatureB64, cert);
            verified = true;
        }

        Map<String, Object> envelope = new LinkedHashMap<>(payload);
        if (hasSig) {
            envelope.put("signatureB64", signatureB64);
            envelope.put("certificatePem", certPem);
        }

        String canonicalJson = writeJson(envelope);

        return new NormalizedAttestation(canonicalJson, payloadJson, subject, ts, hasSig, verified);
    }

    public NormalizedAttestation normalizeOrThrow(Map<String, Object> raw, String expectedHashSha384, long expectedSizeBytes) {
        NormalizedAttestation att = normalizeOrThrow(raw);

        String expHash = expectedHashSha384 == null ? "" : expectedHashSha384.trim();
        if (!expHash.isBlank()) {
            if (raw == null || raw.isEmpty()) {
                throw new IllegalArgumentException("edge_attestation_required");
            }
            Object h = raw.get("hashSha384");
            String got = h == null ? "" : String.valueOf(h).trim();
            if (got.isBlank()) {
                throw new IllegalArgumentException("edge_attestation_missing_hash");
            }
            String a = got.toLowerCase(Locale.ROOT);
            String b = expHash.toLowerCase(Locale.ROOT);
            if (!a.equals(b)) {
                throw new IllegalArgumentException("edge_attestation_hash_mismatch");
            }
        }

        if (expectedSizeBytes > 0) {
            if (raw == null || raw.isEmpty()) {
                throw new IllegalArgumentException("edge_attestation_required");
            }
            Object s = raw.get("size");
            String vs = s == null ? "" : String.valueOf(s).trim();
            if (vs.isBlank()) {
                throw new IllegalArgumentException("edge_attestation_missing_size");
            }
            long gotSize;
            try {
                gotSize = Long.parseLong(vs);
            } catch (Exception e) {
                throw new IllegalArgumentException("edge_attestation_invalid_size");
            }
            if (gotSize != expectedSizeBytes) {
                throw new IllegalArgumentException("edge_attestation_size_mismatch");
            }
        }

        return att;
    }

    private static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return Instant.now();
        try {
            return Instant.parse(s.trim());
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private Map<String, Object> parseRaw(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("edge_attestation_invalid_json", e);
        }
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("edge_attestation_invalid_json", e);
        }
    }

    private static X509Certificate parseX509(String pem) {
        try {
            byte[] der = pemToDer(pem);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
        } catch (Exception e) {
            throw new IllegalArgumentException("edge_attestation_invalid_certificate", e);
        }
    }

    private static void verifySignature(String payloadJson, String signatureB64, X509Certificate cert) {
        byte[] sig;
        try {
            sig = Base64.getDecoder().decode(signatureB64);
        } catch (Exception e) {
            throw new IllegalArgumentException("edge_attestation_invalid_signature_b64", e);
        }

        byte[] msg = payloadJson.getBytes(StandardCharsets.UTF_8);

        List<String> algos = signatureAlgorithms(cert);
        for (String algo : algos) {
            if (algo == null || algo.isBlank()) continue;
            if (tryVerify(algo, cert, msg, sig)) {
                return;
            }
        }

        throw new IllegalArgumentException("edge_attestation_signature_verification_failed");
    }

    private static boolean tryVerify(String algo, X509Certificate cert, byte[] msg, byte[] sig) {
        try {
            Signature verifier = Signature.getInstance(algo);
            verifier.initVerify(cert.getPublicKey());
            verifier.update(msg);
            return verifier.verify(sig);
        } catch (Exception e) {
            return false;
        }
    }

    private static List<String> signatureAlgorithms(X509Certificate cert) {
        String keyAlg = cert.getPublicKey() != null ? cert.getPublicKey().getAlgorithm() : "";
        String ka = keyAlg == null ? "" : keyAlg.toUpperCase(Locale.ROOT);
        if (ka.contains("EC")) {
            return List.of("SHA384withECDSA", "SHA256withECDSA");
        }
        return List.of("SHA384withRSA", "SHA256withRSA");
    }

    private static byte[] pemToDer(String pem) {
        String p = pem.replace("\r", "").trim();
        p = p.replace("-----BEGIN CERTIFICATE-----", "");
        p = p.replace("-----END CERTIFICATE-----", "");
        p = p.replaceAll("\\s+", "");
        return Base64.getDecoder().decode(p);
    }

    private static String s(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    public record NormalizedAttestation(
            String canonicalJson,
            String payloadCanonicalJson,
            String signerSubject,
            Instant signedAt,
            boolean hasSignature,
            boolean signatureVerified
    ) {
    }
}
