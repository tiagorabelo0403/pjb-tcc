package com.tcc.pjb.backend.core.security.crypto.quantum;

import java.security.*;
import java.util.Base64;

public final class PostQuantumSigner {

    private static final String BCPQC_PROVIDER_CLASS = "org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider";
    private static final String BCPQC_PROVIDER_NAME = "BCPQC";

    private final String signatureAlgorithm;
    private final KeyPair keyPair;

    public PostQuantumSigner(String signatureAlgorithm) {
        this.signatureAlgorithm = normalizeAlg(signatureAlgorithm);
        ensureBouncyCastlePqcProviderPresent();
        this.keyPair = generateKeyPair();
    }

    public PqcEvidence sign(byte[] payload) {
        try {
            Signature sig = Signature.getInstance(signatureAlgorithm, BCPQC_PROVIDER_NAME);
            sig.initSign(keyPair.getPrivate(), new SecureRandom());
            sig.update(payload);
            byte[] signature = sig.sign();
            return new PqcEvidence(
                    signatureAlgorithm,
                    Base64.getEncoder().encodeToString(signature),
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
            );
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao assinar com PQC (" + signatureAlgorithm + ")", e);
        }
    }

    public boolean verify(byte[] payload, PqcEvidence evidence) {
        try {
            ensureBouncyCastlePqcProviderPresent();
            byte[] pub = Base64.getDecoder().decode(evidence.publicKeyB64());
            byte[] sigBytes = Base64.getDecoder().decode(evidence.signatureB64());

            KeyFactory kf = KeyFactory.getInstance(guessKeyFactoryAlg(evidence.algorithm()), BCPQC_PROVIDER_NAME);
            PublicKey publicKey = kf.generatePublic(new java.security.spec.X509EncodedKeySpec(pub));

            Signature sig = Signature.getInstance(normalizeAlg(evidence.algorithm()), BCPQC_PROVIDER_NAME);
            sig.initVerify(publicKey);
            sig.update(payload);
            return sig.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }

    private KeyPair generateKeyPair() {
        try {
            
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(signatureAlgorithm, BCPQC_PROVIDER_NAME);
            kpg.initialize(secureDefaultKeySize(signatureAlgorithm), new SecureRandom());
            return kpg.generateKeyPair();
        } catch (GeneralSecurityException e) {
            
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance(signatureAlgorithm, BCPQC_PROVIDER_NAME);
                return kpg.generateKeyPair();
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Não foi possível gerar chave PQC com algoritmo: " + signatureAlgorithm, ex);
            }
        }
    }

    private static int secureDefaultKeySize(String alg) {
        
        
        String a = normalizeAlg(alg);
        if (a.contains("DILITHIUM")) return 5; 
        return 0;
    }

    private static String guessKeyFactoryAlg(String signatureAlg) {
        
        return normalizeAlg(signatureAlg);
    }

    private static String normalizeAlg(String alg) {
        if (alg == null) return "DILITHIUM";
        String a = alg.trim();
        return a.isEmpty() ? "DILITHIUM" : a.toUpperCase();
    }

    private static void ensureBouncyCastlePqcProviderPresent() {
        if (Security.getProvider(BCPQC_PROVIDER_NAME) != null) return;
        try {
            Class<?> clazz = Class.forName(BCPQC_PROVIDER_CLASS);
            Provider p = (Provider) clazz.getConstructor().newInstance();
            Security.addProvider(p);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Provider PQC não encontrado no classpath. Habilite o profile Maven -Ppqc (bcpqc/bcprov) e reinicie a aplicação.",
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao registrar provider PQC (BCPQC).", e);
        }
    }
}
